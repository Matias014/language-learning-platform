import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {CourseEnrollment, UpdateCourseEnrollmentRequest} from '@shared/models';
import {catchError, map, of} from 'rxjs';

@Injectable({providedIn: 'root'})
export class AdminCourseEnrollmentsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  list(params?: { userId?: number; courseId?: number }) {
    if (params?.userId && params?.courseId) {
      return this.http
        .get<CourseEnrollment>(`${this.API}/users/${params.userId}/courses/${params.courseId}/enrollment`)
        .pipe(
          map(e => (e ? [e] as CourseEnrollment[] : [])),
          catchError(err => (err?.status === 404 ? of<CourseEnrollment[]>([]) : (() => {
            throw err;
          })()))
        );
    }
    if (params?.userId) {
      return this.http.get<CourseEnrollment[]>(`${this.API}/users/${params.userId}/enrollments`).pipe(
        catchError(err => (err?.status === 404 ? of<CourseEnrollment[]>([]) : (() => {
          throw err;
        })()))
      );
    }
    if (params?.courseId) {
      return this.http.get<CourseEnrollment[]>(`${this.API}/courses/${params.courseId}/enrollments`).pipe(
        catchError(err => (err?.status === 404 ? of<CourseEnrollment[]>([]) : (() => {
          throw err;
        })()))
      );
    }

    return this.http.get<CourseEnrollment[]>(`${this.API}/enrollments`).pipe(
      catchError(err => (err?.status === 404 ? of<CourseEnrollment[]>([]) : (() => {
        throw err;
      })()))
    );
  }

  get(id: number) {
    return this.http.get<CourseEnrollment>(`${this.API}/enrollments/${id}`);
  }

  update(id: number, patch: UpdateCourseEnrollmentRequest) {
    return this.http.patch<CourseEnrollment>(`${this.API}/enrollments/${id}`, patch);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.API}/enrollments/${id}`);
  }
}
