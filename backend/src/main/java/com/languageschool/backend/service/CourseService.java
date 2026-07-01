package com.languageschool.backend.service;

import com.languageschool.backend.dto.course.CourseDto;
import com.languageschool.backend.dto.course.CreateCourseRequest;
import com.languageschool.backend.dto.course.UpdateCourseRequest;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    CourseDto create(CreateCourseRequest req);

    Optional<CourseDto> findById(Long id);

    List<CourseDto> findAll();

    List<CourseDto> findBy(String learningLanguageCode, String fromLanguageCode, String levelCode);

    CourseDto update(Long id, UpdateCourseRequest req);

    void delete(Long id);

    CourseDto updateCountryIconPath(Long id, String countryIconPath);
}
