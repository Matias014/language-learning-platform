import {InteractionType, LlmStatus} from './enums.model';

export interface LlmLog {
  id: number;
  userId: number | null;
  lessonId: number | null;
  exerciseAttemptId: number | null;
  chatSessionId: number | null;
  interactionType: InteractionType;
  model: string;
  tokensIn: number | null;
  tokensOut: number | null;
  latencyMs: number | null;
  params: Record<string, unknown> | null;
  status: LlmStatus;
  prompt: string;
  response: string;
  createdAt: string;
}
