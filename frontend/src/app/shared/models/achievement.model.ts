export interface Achievement {
  id: number;
  title: string;
  description: string | null;
  iconPath: string | null;
  requiredXp: number | null;
}

export interface CreateAchievementRequest {
  title: string;
  description?: string | null;
  iconPath?: string | null;
  requiredXp: number;
}

export interface UpdateAchievementRequest {
  title?: string;
  description?: string | null;
  iconPath?: string | null;
  requiredXp?: number | null;
}
