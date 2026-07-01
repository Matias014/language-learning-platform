package com.languageschool.backend.repository;

import com.languageschool.backend.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByLesson_Id(Long lessonId);

    long countByLesson_Course_Id(Long courseId);

    long countByLesson_Id(Long lessonId);
}
