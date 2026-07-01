package com.languageschool.backend.repository;

import com.languageschool.backend.entity.CourseRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRecommendationRepository extends JpaRepository<CourseRecommendation, Long> {

    void deleteByUser_Id(Long userId);

}
