export interface ChatSession {
  id: number;
  userId: number;
  conversationLanguageCode: string | null;
  title: string | null;
  systemPrompt: string | null;
  startedAt: string;
}

export interface CreateChatSessionRequest {
  conversationLanguageCode?: string | null;
  title?: string | null;
}

export interface UpdateChatSessionRequest {
  conversationLanguageCode?: string | null;
  title?: string | null;
}
