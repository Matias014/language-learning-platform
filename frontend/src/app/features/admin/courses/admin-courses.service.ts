import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Course, CreateCourseRequest, UpdateCourseRequest} from '@app/shared/models/course.model';
import {environment} from 'environments/environment';

@Injectable({providedIn: 'root'})
export class AdminCoursesService {
  private readonly http = inject(HttpClient);
  private readonly API = environment.apiUrl;

  list(): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.API}/courses`);
  }

  get(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.API}/courses/${id}`);
  }

  create(body: CreateCourseRequest): Observable<Course> {
    return this.http.post<Course>(`${this.API}/courses`, body);
  }

  update(id: number, patch: UpdateCourseRequest): Observable<Course> {
    return this.http.patch<Course>(`${this.API}/courses/${id}`, patch);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/courses/${id}`);
  }

  uploadCountryIcon(id: number, file: File): Observable<Course> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.put<Course>(`${this.API}/courses/${id}/country-icon`, fd);
  }
}
