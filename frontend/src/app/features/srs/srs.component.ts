import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router} from '@angular/router';
import {catchError} from 'rxjs/operators';
import {of} from 'rxjs';
import {SrsService} from './srs.service';
import {Exercise, Lesson, UserSrs} from '@shared/models';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';

type ToastType = 'success' | 'error';

type Ymd = {year: number; month: number; day: number};

@Component({
  standalone: true,
  selector: 'app-srs',
  imports: [CommonModule, TPipe],
  templateUrl: './srs.component.html',
  styleUrls: ['./srs.component.scss'],
})
export class SrsComponent implements OnInit {
  private api = inject(SrsService);
  private router = inject(Router);
  private i18n = inject(I18nService);

  private readonly TZ = 'Europe/Warsaw';

  loading = signal(true);
  error = signal<string | null>(null);

  dueSrs = signal<UserSrs[]>([]);
  upcomingSrs = signal<UserSrs[]>([]);
  exerciseMap = signal<Map<number, Exercise>>(new Map());
  lessonMap = signal<Map<number, Lesson>>(new Map());
  empty = computed(() => !this.dueSrs().length && !this.upcomingSrs().length);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit() {
    this.loading.set(true);
    this.error.set(null);

    const todayYmd = this.ymdInZone(new Date(), this.TZ);
    const startTomorrow = this.startOfDayInZone(this.addDays(todayYmd, 1), this.TZ);
    const beforeExclusive = this.startOfDayInZone(this.addDays(todayYmd, 32), this.TZ);

    this.api
      .listSrsDue(beforeExclusive.toISOString())
      .pipe(
        catchError(err => {
          const msg =
            this.i18n.getServerErrorMessage(err, 'srs.errors.loadFailed') ||
            this.i18n.t('srs.errors.loadFailed');
          this.error.set('srs.errors.loadFailed');
          this.showError(msg);
          this.loading.set(false);
          return of([] as UserSrs[]);
        })
      )
      .subscribe(all => {
        const due: UserSrs[] = [];
        const upcoming: UserSrs[] = [];
        const tomorrowMs = startTomorrow.getTime();

        for (const s of all || []) {
          const d = new Date(s.dueAt).getTime();
          if (Number.isFinite(d) && d < tomorrowMs) {
            due.push(s);
          } else {
            upcoming.push(s);
          }
        }

        this.dueSrs.set(due);
        this.upcomingSrs.set(upcoming);

        const exerciseIds = Array.from(new Set((all || []).map(s => s.exerciseId)));

        if (!exerciseIds.length) {
          this.exerciseMap.set(new Map());
          this.lessonMap.set(new Map());
          this.loading.set(false);
          return;
        }

        this.api
          .loadExercises(exerciseIds)
          .pipe(
            catchError(err => {
              const msg =
                this.i18n.getServerErrorMessage(err, 'srs.errors.loadFailed') ||
                this.i18n.t('srs.errors.loadFailed');
              this.error.set('srs.errors.loadFailed');
              this.showError(msg);
              this.loading.set(false);
              return of(new Map<number, Exercise>());
            })
          )
          .subscribe(exMap => {
            this.exerciseMap.set(exMap);

            const lessonIds = Array.from(new Set(Array.from(exMap.values()).map(e => e.lessonId)));

            if (!lessonIds.length) {
              this.lessonMap.set(new Map());
              this.loading.set(false);
              return;
            }

            this.api
              .loadLessons(lessonIds)
              .pipe(
                catchError(err => {
                  const msg =
                    this.i18n.getServerErrorMessage(err, 'srs.errors.loadFailed') ||
                    this.i18n.t('srs.errors.loadFailed');
                  this.error.set('srs.errors.loadFailed');
                  this.showError(msg);
                  this.loading.set(false);
                  return of(new Map<number, Lesson>());
                })
              )
              .subscribe(lesMap => {
                this.lessonMap.set(lesMap);
                this.loading.set(false);
              });
          });
      });
  }

  exFor(s: UserSrs): Exercise | undefined {
    return this.exerciseMap().get(s.exerciseId);
  }

  lessonFor(s: UserSrs): Lesson | undefined {
    const ex = this.exFor(s);
    return ex ? this.lessonMap().get(ex.lessonId) : undefined;
  }

  nextDueISO(s: UserSrs): string {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: this.TZ,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date(s.dueAt));
  }

  openLesson(s: UserSrs) {
    const ex = this.exFor(s);
    if (!ex) return;
    this.router.navigate(['/lessons', ex.lessonId], {queryParams: {focus: ex.id}});
  }

  trackBySrsId(_: number, s: UserSrs) {
    return s.id;
  }

  private showToast(text: string, type: ToastType) {
    const id = ++this.toastSeq;
    this.toasts.set([{id, text, type}, ...this.toasts()]);
    setTimeout(() => this.dismissToast(id), 4000);
  }

  showError(text: string) {
    if (!text) return;
    this.showToast(text, 'error');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }

  private part(parts: Intl.DateTimeFormatPart[], type: string): string {
    return parts.find(p => p.type === type)?.value ?? '';
  }

  private ymdInZone(date: Date, timeZone: string): Ymd {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(date);

    return {
      year: Number(this.part(parts, 'year')),
      month: Number(this.part(parts, 'month')),
      day: Number(this.part(parts, 'day')),
    };
  }

  private addDays(ymd: Ymd, days: number): Ymd {
    const d = new Date(Date.UTC(ymd.year, ymd.month - 1, ymd.day + days, 12, 0, 0));
    return {year: d.getUTCFullYear(), month: d.getUTCMonth() + 1, day: d.getUTCDate()};
  }

  private getTimeZoneOffsetMinutes(date: Date, timeZone: string): number {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(date);

    const year = Number(this.part(parts, 'year'));
    const month = Number(this.part(parts, 'month'));
    const day = Number(this.part(parts, 'day'));
    const hour = Number(this.part(parts, 'hour'));
    const minute = Number(this.part(parts, 'minute'));
    const second = Number(this.part(parts, 'second'));

    const asUtc = Date.UTC(year, month - 1, day, hour, minute, second);
    return (asUtc - date.getTime()) / 60000;
  }

  private zonedTimeToUtcMillis(ymd: Ymd, hour: number, minute: number, second: number, timeZone: string): number {
    const utcGuess = Date.UTC(ymd.year, ymd.month - 1, ymd.day, hour, minute, second);
    let date = new Date(utcGuess);
    let offset = this.getTimeZoneOffsetMinutes(date, timeZone);
    let utc = utcGuess - offset * 60000;

    date = new Date(utc);
    const offset2 = this.getTimeZoneOffsetMinutes(date, timeZone);
    if (offset2 !== offset) {
      utc = utcGuess - offset2 * 60000;
    }

    return utc;
  }

  private startOfDayInZone(ymd: Ymd, timeZone: string): Date {
    return new Date(this.zonedTimeToUtcMillis(ymd, 0, 0, 0, timeZone));
  }
}
