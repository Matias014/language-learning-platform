package com.languageschool.backend.repository;

import com.languageschool.backend.entity.LessonStatus;
import com.languageschool.backend.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByUser_IdAndLesson_Id(Long userId, Long lessonId);

    List<UserLessonProgress> findByUser_Id(Long userId);

    List<UserLessonProgress> findByUser_IdAndStatus(Long userId, LessonStatus status);

    List<UserLessonProgress> findByLesson_Id(Long lessonId);
}
