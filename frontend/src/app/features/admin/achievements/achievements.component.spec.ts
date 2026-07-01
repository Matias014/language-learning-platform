import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminAchievementsComponent } from './achievements.component';

describe('AdminAchievementsComponent', () => {
  let component: AdminAchievementsComponent;
  let fixture: ComponentFixture<AdminAchievementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminAchievementsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminAchievementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
