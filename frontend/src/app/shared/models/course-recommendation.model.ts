export interface CourseRecommendation {
  id: number;
  userId: number;
  courseId: number;
  score: number;
  createdAt: string;
}

export interface GenerateCourseRecommendationsRequest {
  limit?: number;
  learningLanguageCode?: string;
  fromLanguageCode?: string;
  levelCode?: string;
}
