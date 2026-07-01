package com.languageschool.backend.service;

import com.languageschool.backend.dto.courseProgress.CourseProgressDto;

import java.util.List;

public interface CourseProgressService {

    void recalcEnrollment(Long enrollmentId);

    void recalcForUserCourse(Long userId, Long courseId);

    void recalcAllForCourse(Long courseId);

    void touchLastActivity(Long userId, Long courseId);

    List<CourseProgressDto> listMy(String login);

    CourseProgressDto getMyForCourse(Long courseId, String login);
}
