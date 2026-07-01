package com.languageschool.backend.service;

import com.languageschool.backend.dto.courseEnrollment.CourseEnrollmentDto;
import com.languageschool.backend.dto.courseEnrollment.CreateCourseEnrollmentRequest;
import com.languageschool.backend.dto.courseEnrollment.UpdateCourseEnrollmentRequest;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentService {

    CourseEnrollmentDto create(CreateCourseEnrollmentRequest req);

    CourseEnrollmentDto getById(Long id);

    Optional<CourseEnrollmentDto> findByUserAndCourse(Long userId, Long courseId);

    List<CourseEnrollmentDto> findByUser(Long userId);

    List<CourseEnrollmentDto> findByCourse(Long courseId);

    CourseEnrollmentDto update(Long id, UpdateCourseEnrollmentRequest req);

    void delete(Long id);

    List<CourseEnrollmentDto> findAll();
}
