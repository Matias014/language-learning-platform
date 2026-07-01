import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminAwardsComponent } from './exercise-awards.component';

describe('AdminAwardsComponent', () => {
  let component: AdminAwardsComponent;
  let fixture: ComponentFixture<AdminAwardsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminAwardsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminAwardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
