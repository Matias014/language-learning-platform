package com.languageschool.backend.repository;

import com.languageschool.backend.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    Optional<CourseEnrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    List<CourseEnrollment> findByUser_Id(Long userId);

    List<CourseEnrollment> findByCourse_Id(Long courseId);

    List<CourseEnrollment> findByCourse_IdIn(Iterable<Long> courseIds);

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);
}
