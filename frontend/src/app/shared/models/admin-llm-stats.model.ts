import {InteractionType, LlmStatus} from './enums.model';

export interface AdminLlmStats {
  calls: number;
  tokensIn: number;
  tokensOut: number;
  averageLatencyMs: number | null;
  callsByInteractionType: Partial<Record<InteractionType, number>>;
  callsByStatus: Partial<Record<LlmStatus, number>>;
}
