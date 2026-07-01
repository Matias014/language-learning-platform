export interface XpSummary {
  totalXp: number;
  awardsCount: number;
}

export interface XpPoint {
  date: string;
  xp: number;
}

export interface XpBreakdownItem {
  key: string;
  xp: number;
}

export interface XpBreakdown {
  days: number;
  byType: Record<string, number>;
  byDifficulty: Record<string, number>;
}
