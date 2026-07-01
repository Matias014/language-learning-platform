package com.languageschool.backend.repository;

import com.languageschool.backend.entity.ProficiencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProficiencyLevelRepository extends JpaRepository<ProficiencyLevel, String> {

}