import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AdminCourseEnrollmentsComponent} from './course-enrollments.component';

describe('AdminCourseEnrollmentsComponent', () => {
  let component: AdminCourseEnrollmentsComponent;
  let fixture: ComponentFixture<AdminCourseEnrollmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCourseEnrollmentsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCourseEnrollmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
