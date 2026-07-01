package com.languageschool.backend.repository;

import com.languageschool.backend.entity.ExerciseAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExerciseAwardRepository extends JpaRepository<ExerciseAward, Long> {
    Optional<ExerciseAward> findTopByAttempt_User_IdAndAttempt_Exercise_IdOrderByAwardedAtDesc(Long userId, Long exerciseId);

    Optional<ExerciseAward> findByAttempt_Id(Long attemptId);

    List<ExerciseAward> findByAttempt_User_Id(Long userId);

    List<ExerciseAward> findByAttempt_User_IdAndAwardedAtBetween(Long userId, Instant start, Instant end);

    @Query("select count(distinct ea.attempt.exercise.id) from ExerciseAward ea where ea.attempt.user.id = :userId and ea.attempt.exercise.lesson.course.id = :courseId")
    long countDistinctExercisesByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("select count(distinct ea.attempt.exercise.id) from ExerciseAward ea where ea.attempt.user.id = :userId and ea.attempt.exercise.lesson.id = :lessonId")
    long countDistinctExercisesByUserAndLesson(@Param("userId") Long userId, @Param("lessonId") Long lessonId);
}
