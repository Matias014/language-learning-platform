import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AdminUserLessonProgressComponent} from './user-lesson-progress.component';

describe('AdminUserLessonProgressComponent', () => {
  let component: AdminUserLessonProgressComponent;
  let fixture: ComponentFixture<AdminUserLessonProgressComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUserLessonProgressComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminUserLessonProgressComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
