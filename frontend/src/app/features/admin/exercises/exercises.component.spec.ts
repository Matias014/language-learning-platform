import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AdminExercisesComponent} from './exercises.component';

describe('AdminExercisesComponent', () => {
  let component: AdminExercisesComponent;
  let fixture: ComponentFixture<AdminExercisesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminExercisesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminExercisesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
