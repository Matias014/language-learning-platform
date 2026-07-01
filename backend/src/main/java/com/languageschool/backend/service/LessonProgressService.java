package com.languageschool.backend.service;

import com.languageschool.backend.dto.lessonProgress.LessonProgressDto;

import java.util.List;

public interface LessonProgressService {
    LessonProgressDto getMyForLesson(Long lessonId, String login);

    List<LessonProgressDto> listMyByCourse(Long courseId, String login);
}
