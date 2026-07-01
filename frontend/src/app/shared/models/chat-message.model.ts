import {MessageSender} from './enums.model';

export interface ChatMessage {
  id: number;
  sessionId: number;
  sender: MessageSender;
  message: string;
  sentAt: string;
}

export interface CreateChatMessageRequest {
  message: string;
}

export interface UpdateChatMessageRequest {
  message: string;
}
