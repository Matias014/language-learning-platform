package com.languageschool.backend.service;

import com.languageschool.backend.dto.userLessonProgress.CreateUserLessonProgressRequest;
import com.languageschool.backend.dto.userLessonProgress.UpdateUserLessonProgressRequest;
import com.languageschool.backend.dto.userLessonProgress.UserLessonProgressDto;
import com.languageschool.backend.entity.LessonStatus;

import java.util.List;
import java.util.Optional;

public interface UserLessonProgressService {

    UserLessonProgressDto create(CreateUserLessonProgressRequest req);

    Optional<UserLessonProgressDto> findById(Long id);

    Optional<UserLessonProgressDto> findByUserAndLesson(Long userId, Long lessonId);

    List<UserLessonProgressDto> findByUser(Long userId);

    List<UserLessonProgressDto> findByUser(Long userId, LessonStatus status);

    List<UserLessonProgressDto> findByLesson(Long lessonId);

    UserLessonProgressDto update(Long id, UpdateUserLessonProgressRequest req);

    void delete(Long id);

    List<UserLessonProgressDto> findAll();
}
