export interface Level {
  level: number;
  requiredXp: number;
}

export interface CreateLevelRequest {
  level: number;
  requiredXp: number;
}

export interface UpdateLevelRequest {
  requiredXp?: number;
}

export interface LevelSummary {
  currentLevel: number;
  percentToNext: number;
}
