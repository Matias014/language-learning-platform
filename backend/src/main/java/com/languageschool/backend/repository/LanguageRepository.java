package com.languageschool.backend.repository;

import com.languageschool.backend.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, String> {

}
