package com.languageschool.backend.service;

import com.languageschool.backend.dto.lesson.CreateLessonRequest;
import com.languageschool.backend.dto.lesson.LessonDto;
import com.languageschool.backend.dto.lesson.UpdateLessonRequest;

import java.util.List;
import java.util.Optional;

public interface LessonService {

    LessonDto create(CreateLessonRequest req);

    Optional<LessonDto> findById(Long id);

    List<LessonDto> findByCourse(Long courseId);

    LessonDto update(Long id, UpdateLessonRequest req);

    void delete(Long id);
}
