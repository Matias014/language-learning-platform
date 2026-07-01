export interface Course {
  id: number;
  learningLanguageCode: string;
  fromLanguageCode: string;
  title: string;
  description: string | null;
  levelCode: string;
  countryIconPath: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCourseRequest {
  learningLanguageCode: string;
  fromLanguageCode: string;
  title: string;
  description?: string | null;
  levelCode: string;
  countryIconPath?: string | null;
}

export interface UpdateCourseRequest {
  learningLanguageCode?: string;
  fromLanguageCode?: string;
  title?: string;
  description?: string | null;
  levelCode?: string;
  countryIconPath?: string | null;
}
