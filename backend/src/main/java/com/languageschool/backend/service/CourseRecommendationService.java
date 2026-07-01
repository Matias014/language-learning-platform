package com.languageschool.backend.service;

import com.languageschool.backend.dto.courseRecommendation.CourseRecommendationDto;

import java.util.List;

public interface CourseRecommendationService {

    List<CourseRecommendationDto> generateForUser(Long userId, Integer limit, String learningLanguageCode, String fromLanguageCode, String levelCode);

    List<CourseRecommendationDto> findTopForUser(Long userId, Integer limit);
}
