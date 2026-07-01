package com.languageschool.backend.repository;

import com.languageschool.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
              select c from Course c
              left join c.proficiencyLevel l
              where (:lang is null or c.learningLanguage.code = :lang)
                and (:src  is null or c.fromLanguage.code = :src)
                and (:lvl  is null or l.code = :lvl)
              order by c.title asc
            """)
    List<Course> findByFilters(@Param("lang") String learningLanguageCode,
                               @Param("src") String fromLanguageCode,
                               @Param("lvl") String levelCode);
}
