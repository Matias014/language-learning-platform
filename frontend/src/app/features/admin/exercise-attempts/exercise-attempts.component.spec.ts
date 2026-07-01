import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminAttemptsComponent } from './exercise-attempts.component';

describe('AdminAttemptsComponent', () => {
  let component: AdminAttemptsComponent;
  let fixture: ComponentFixture<AdminAttemptsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminAttemptsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminAttemptsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
