import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Course, CourseEnrollment, Lesson, CourseProgress} from '@shared/models';

@Injectable({providedIn: 'root'})
export class CoursesService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listCourses(): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.api}/courses`);
  }

  getCourse(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.api}/courses/${id}`);
  }

  listLessonsForCourse(courseId: number): Observable<Lesson[]> {
    return this.http.get<Lesson[]>(`${this.api}/courses/${courseId}/lessons`);
  }

  myEnrollments(): Observable<CourseEnrollment[]> {
    return this.http.get<CourseEnrollment[]>(`${this.api}/users/me/enrollments`);
  }

  getMyEnrollmentForCourse(courseId: number): Observable<CourseEnrollment> {
    return this.http.get<CourseEnrollment>(`${this.api}/users/me/courses/${courseId}/enrollment`);
  }

  enroll(courseId: number): Observable<CourseEnrollment> {
    return this.http.post<CourseEnrollment>(`${this.api}/enrollments`, {courseId});
  }

  unenroll(enrollmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/enrollments/${enrollmentId}`);
  }

  listMyCourseProgress(): Observable<CourseProgress[]> {
    return this.http.get<CourseProgress[]>(`${this.api}/users/me/courses/progress`);
  }

  getMyCourseProgress(courseId: number): Observable<CourseProgress> {
    return this.http.get<CourseProgress>(`${this.api}/users/me/courses/${courseId}/progress`);
  }
}
