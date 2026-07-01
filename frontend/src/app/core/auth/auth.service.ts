import {Injectable, inject, signal, computed} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {catchError, map, switchMap, tap} from 'rxjs/operators';
import {environment} from 'environments/environment';
import {User} from '@shared/models/user.model';
import {LoginRequest, LoginResponse, RefreshTokenResponse} from '@shared/models/auth.model';
import {CreateUserRequest} from '@shared/models/user.model';

@Injectable({providedIn: 'root'})
export class AuthService {
  private http = inject(HttpClient);
  private readonly API = environment.apiUrl;
  private readonly TOKEN_KEY = 'jwt';
  private readonly EXP_KEY = 'jwt_exp';
  private readonly ROLE_KEY = 'role';
  private readonly USER_KEY = 'user';
  private userSig = signal<User | null>(this.loadStoredUser());
  private tokenSig = signal<string | null>(this.loadStoredToken());
  private logoutTimer: any = null;
  private refreshTimer: any = null;

  readonly user = this.userSig.asReadonly();
  readonly isAuth = computed(() => this.isAuthenticated());
  readonly role = computed(() => this.userSig()?.role ?? null);
  readonly username = computed(() => this.userSig()?.login || this.userSig()?.email || '');
  readonly avatarPath = computed(() => this.userSig()?.avatarPath || '');

  constructor() {
    this.reconcileStorage();
    const expMillis = this.getStoredExp();
    if (this.tokenSig() && expMillis && Date.now() < expMillis) {
      this.scheduleTimers(expMillis);
    }
  }

  login(body: LoginRequest, opts: { remember: boolean }): Observable<void> {
    return this.http.post<LoginResponse>(`${this.API}/auth/login`, body, {withCredentials: true}).pipe(
      tap(res => this.storeAuthOnLogin(res, opts.remember)),
      switchMap(() => this.me(true)),
      map(() => void 0)
    );
  }

  register(body: CreateUserRequest, opts: { remember: boolean }): Observable<void> {
    return this.http.post<LoginResponse>(`${this.API}/auth/register`, body, {withCredentials: true}).pipe(
      tap(res => this.storeAuthOnLogin(res, opts.remember)),
      switchMap(() => this.me(true)),
      map(() => void 0)
    );
  }

  refreshAccessToken(): Observable<boolean> {
    return this.http.post<RefreshTokenResponse>(`${this.API}/auth/refresh`, {}, {withCredentials: true}).pipe(
      tap(res => this.storeAuthOnRefresh(res)),
      map(() => true),
      catchError(() => {
        this.clearLocalAuth();
        return of(false);
      })
    );
  }

  me(force = false): Observable<User | null> {
    const cached = this.userSig();
    if (!force && cached) {
      return of(cached);
    }
    if (!this.isAuthenticated()) {
      this.storeUserAndRole(null);
      return of(null);
    }
    return this.http.get<User>(`${this.API}/users/me`).pipe(
      tap(u => this.storeUserAndRole(u)),
      catchError(() => {
        this.storeUserAndRole(null);
        return of(null);
      })
    );
  }

  logout(): void {
    this.http.post(`${this.API}/auth/logout`, {}, {withCredentials: true}).pipe(catchError(() => of(void 0))).subscribe();
    this.clearLocalAuth();
  }

  isAuthenticated(): boolean {
    const token = this.tokenSig();
    const expMillis = this.getStoredExp();
    return !!token && !!expMillis && Date.now() < expMillis;
  }

  getToken(): string | null {
    return this.tokenSig();
  }

  private storeAuthOnLogin(res: LoginResponse, remember: boolean): void {
    const storage = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    const expMillis = Date.now() + Number(res.expiresIn) * 1000;
    other.removeItem(this.TOKEN_KEY);
    other.removeItem(this.EXP_KEY);
    other.removeItem(this.ROLE_KEY);
    other.removeItem(this.USER_KEY);
    storage.setItem(this.TOKEN_KEY, res.accessToken);
    storage.setItem(this.EXP_KEY, String(expMillis));
    this.tokenSig.set(res.accessToken);
    this.scheduleTimers(expMillis);
  }

  private storeAuthOnRefresh(res: RefreshTokenResponse): void {
    const storage = localStorage.getItem(this.TOKEN_KEY) ? localStorage : sessionStorage;
    const other = storage === localStorage ? sessionStorage : localStorage;
    const expMillis = Date.now() + Number(res.expiresIn) * 1000;
    other.removeItem(this.TOKEN_KEY);
    other.removeItem(this.EXP_KEY);
    other.removeItem(this.ROLE_KEY);
    other.removeItem(this.USER_KEY);
    storage.setItem(this.TOKEN_KEY, res.accessToken);
    storage.setItem(this.EXP_KEY, String(expMillis));
    this.tokenSig.set(res.accessToken);
    this.scheduleTimers(expMillis);
  }

  private storeUserAndRole(u: User | null): void {
    const storage = localStorage.getItem(this.TOKEN_KEY) ? localStorage : sessionStorage;
    const other = storage === localStorage ? sessionStorage : localStorage;
    other.removeItem(this.USER_KEY);
    other.removeItem(this.ROLE_KEY);
    if (u) {
      storage.setItem(this.USER_KEY, JSON.stringify(u));
      storage.setItem(this.ROLE_KEY, String(u.role));
      this.userSig.set(u);
    } else {
      storage.removeItem(this.USER_KEY);
      storage.removeItem(this.ROLE_KEY);
      this.userSig.set(null);
    }
  }

  private scheduleTimers(expMillis: number): void {
    this.clearTimers();
    const now = Date.now();
    const ttl = Math.max(0, expMillis - now);
    const refreshDelay = Math.max(0, ttl - 30000);
    this.refreshTimer = setTimeout(() => this.refreshAccessToken().subscribe(), refreshDelay);
    this.logoutTimer = setTimeout(() => this.logout(), ttl + 5000);
  }

  private loadStoredToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY) ?? localStorage.getItem(this.TOKEN_KEY);
  }

  private getStoredExp(): number | null {
    const expStr = sessionStorage.getItem(this.EXP_KEY) ?? localStorage.getItem(this.EXP_KEY);
    return expStr ? Number(expStr) : null;
  }

  private loadStoredUser(): User | null {
    const raw = sessionStorage.getItem(this.USER_KEY) ?? localStorage.getItem(this.USER_KEY);
    try {
      return raw ? (JSON.parse(raw) as User) : null;
    } catch {
      return null;
    }
  }

  private clearLocalAuth(): void {
    for (const s of [localStorage, sessionStorage]) {
      s.removeItem(this.TOKEN_KEY);
      s.removeItem(this.EXP_KEY);
      s.removeItem(this.ROLE_KEY);
      s.removeItem(this.USER_KEY);
    }
    this.tokenSig.set(null);
    this.userSig.set(null);
    this.clearTimers();
  }

  private clearTimers(): void {
    if (this.logoutTimer) {
      clearTimeout(this.logoutTimer);
      this.logoutTimer = null;
    }
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  private reconcileStorage(): void {
    const tokenLocal = localStorage.getItem(this.TOKEN_KEY);
    const tokenSession = sessionStorage.getItem(this.TOKEN_KEY);
    if (tokenLocal && tokenSession) {
      sessionStorage.removeItem(this.TOKEN_KEY);
      sessionStorage.removeItem(this.EXP_KEY);
      sessionStorage.removeItem(this.ROLE_KEY);
      sessionStorage.removeItem(this.USER_KEY);
      this.tokenSig.set(tokenLocal);
      return;
    }
    if (tokenLocal) {
      sessionStorage.removeItem(this.EXP_KEY);
      sessionStorage.removeItem(this.ROLE_KEY);
      sessionStorage.removeItem(this.USER_KEY);
      return;
    }
    if (tokenSession) {
      localStorage.removeItem(this.EXP_KEY);
      localStorage.removeItem(this.ROLE_KEY);
      localStorage.removeItem(this.USER_KEY);
    }
  }
}
