import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChatSessionsComponent } from './chat-sessions.component';

describe('ChatSessionsComponent', () => {
  let component: ChatSessionsComponent;
  let fixture: ComponentFixture<ChatSessionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatSessionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChatSessionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
