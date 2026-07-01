export interface UserSrs {
  id: number;
  userId: number;
  exerciseId: number;
  dueAt: string;
  intervalDays: number;
  repetitions: number;
  lastQuality: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewUserSrsRequest {
  exerciseId: number;
  quality: number;
}

export interface SrsWeekDay {
  date: string;
  count: number;
}

export interface SrsThisWeek {
  dueTotal: number;
  byDay: SrsWeekDay[];
}

export interface SrsSummary {
  dueTodayCount: number;
  dueNext7DaysCount: number;
}
