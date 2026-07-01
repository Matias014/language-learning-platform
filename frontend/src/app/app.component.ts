import {Component, computed, inject} from '@angular/core';
import {NgIf} from '@angular/common';
import {Router, NavigationEnd, ActivatedRouteSnapshot, RouterOutlet} from '@angular/router';
import {NavbarComponent} from '@shared/components/navbar/navbar.component';
import {toSignal} from '@angular/core/rxjs-interop';
import {filter, map, startWith} from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NgIf, RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'LanguageSchoolFrontend';
  private router = inject(Router);

  private deepestData = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.getDeepest(this.router.routerState.snapshot.root).data),
      startWith(this.getDeepest(this.router.routerState.snapshot.root).data)
    ),
    {initialValue: this.getDeepest(this.router.routerState.snapshot.root).data}
  );

  hideNavbar = computed(() => this.deepestData()['hideNavbar'] === true);

  private getDeepest(route: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    let r = route;
    while (r.firstChild) r = r.firstChild;
    return r;
  }
}
