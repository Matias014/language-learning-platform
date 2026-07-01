export type UserRole = 'user' | 'admin';
export type MessageSender = 'user' | 'ai';
export type ExerciseType = 'quiz' | 'fill_in' | 'writing';
export type DifficultyLevel = 'easy' | 'medium' | 'hard';
export type CourseStatus = 'in_progress' | 'completed';
export type LessonStatus = 'in_progress' | 'completed';
export type LlmStatus = 'ok' | 'timeout' | 'rate_limited' | 'quota_exceeded' | 'safety_block' | 'invalid_request' | 'server_error';
export type InteractionType = 'chat' | 'grading' | 'generation' | 'hint';
