import {ComponentFixture, TestBed} from '@angular/core/testing';

import {AdminLlmLogsComponent} from './llm-logs.component';

describe('AdminLlmLogsComponent', () => {
  let component: AdminLlmLogsComponent;
  let fixture: ComponentFixture<AdminLlmLogsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminLlmLogsComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminLlmLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
