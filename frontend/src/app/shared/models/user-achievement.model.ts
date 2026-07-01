export interface UserAchievement {
  id: number;
  userId: number;
  achievementId: number;
  earnedAt: string;
}

export interface CreateUserAchievementRequest {
  achievementId: number;
}
