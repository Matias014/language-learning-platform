import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminLevelsComponent } from './levels.component';

describe('AdminLevelsComponent', () => {
  let component: AdminLevelsComponent;
  let fixture: ComponentFixture<AdminLevelsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminLevelsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminLevelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
