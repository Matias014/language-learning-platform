create unique index if not exists uq_courses_active
    on courses (learning_language_code, from_language_code, level_code, title)
    where deleted_at is null;

create unique index if not exists uq_lessons_active
    on lessons (course_id, order_number)
    where deleted_at is null;

create unique index if not exists uq_exercises_active
    on exercises (lesson_id, order_number)
    where deleted_at is null;

create unique index if not exists uq_exercise_options_active
    on exercise_options (exercise_id, order_number)
    where deleted_at is null;

create unique index if not exists uq_achievements_active
    on achievements (title)
    where deleted_at is null;

create unique index if not exists uq_users_login_active
    on users (login)
    where deleted_at is null;

create unique index if not exists uq_users_email_active
    on users (email)
    where deleted_at is null;

create unique index if not exists uq_languages_name_active
    on languages (name)
    where deleted_at is null;

create unique index if not exists uq_proficiency_levels_name_active
    on proficiency_levels (name)
    where deleted_at is null;

create unique index if not exists uq_proficiency_levels_order_active
    on proficiency_levels (order_number)
    where deleted_at is null;

create unique index if not exists uq_course_enrollments_active
    on course_enrollments (user_id, course_id)
    where deleted_at is null;

create unique index if not exists uq_user_lesson_progress_active
    on user_lesson_progress (user_id, lesson_id)
    where deleted_at is null;

create unique index if not exists uq_user_achievements_active
    on user_achievements (user_id, achievement_id)
    where deleted_at is null;

create unique index if not exists uq_exercise_awards_active
    on exercise_awards (attempt_id)
    where deleted_at is null;

create unique index if not exists uq_exercise_attempts_triplet_active
    on exercise_attempts (user_id, exercise_id, attempt_number)
    where deleted_at is null;

create unique index if not exists uq_course_recommendations_active
    on course_recommendations (user_id, course_id)
    where deleted_at is null;

create unique index if not exists uq_user_srs_active
    on user_srs (user_id, exercise_id)
    where deleted_at is null;

create unique index if not exists uq_user_levels_required_xp
    on user_levels (required_xp);

create unique index if not exists uq_refresh_tokens_hash
    on refresh_tokens (token_hash);
