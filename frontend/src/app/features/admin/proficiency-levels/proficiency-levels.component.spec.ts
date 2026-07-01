import {ComponentFixture, TestBed} from '@angular/core/testing';

import {AdminProficiencyLevelsComponent} from './proficiency-levels.component';

describe('AdminProficiencyLevelsComponent', () => {
  let component: AdminProficiencyLevelsComponent;
  let fixture: ComponentFixture<AdminProficiencyLevelsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminProficiencyLevelsComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminProficiencyLevelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
