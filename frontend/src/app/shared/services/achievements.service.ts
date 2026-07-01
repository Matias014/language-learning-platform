import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Achievement, UserAchievement} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AchievementsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listAll(): Observable<Achievement[]> {
    return this.http.get<Achievement[]>(`${this.api}/achievements`);
  }

  listMine(): Observable<UserAchievement[]> {
    return this.http.get<UserAchievement[]>(`${this.api}/users/me/achievements`);
  }
}
