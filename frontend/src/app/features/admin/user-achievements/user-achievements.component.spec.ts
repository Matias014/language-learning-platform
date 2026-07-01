import {ComponentFixture, TestBed} from '@angular/core/testing';

import {AdminUserAchievementsComponent} from './user-achievements.component';

describe('AdminUserAchievementsComponent', () => {
  let component: AdminUserAchievementsComponent;
  let fixture: ComponentFixture<AdminUserAchievementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUserAchievementsComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminUserAchievementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
