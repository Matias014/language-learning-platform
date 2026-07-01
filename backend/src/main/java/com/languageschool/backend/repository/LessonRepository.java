package com.languageschool.backend.repository;

import com.languageschool.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourse_IdOrderByOrderNumberAsc(Long courseId);
}
