package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.course.CourseDto;
import com.languageschool.backend.dto.course.CreateCourseRequest;
import com.languageschool.backend.dto.course.UpdateCourseRequest;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.util.SecurityUtils;
import com.languageschool.backend.service.CourseService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepo;
    private final LanguageRepository languageRepo;
    private final ProficiencyLevelRepository levelRepo;

    public CourseServiceImpl(CourseRepository courseRepo,
                             LanguageRepository languageRepo,
                             ProficiencyLevelRepository levelRepo) {
        this.courseRepo = courseRepo;
        this.languageRepo = languageRepo;
        this.levelRepo = levelRepo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Transactional
    @Override
    public CourseDto create(CreateCourseRequest req) {
        SecurityUtils.requireAdmin(auth());

        String learningCode = trim(req.getLearningLanguageCode());
        String fromCode = trim(req.getFromLanguageCode());
        String levelCode = trim(req.getLevelCode());
        String title = trim(req.getTitle());

        if (title == null || title.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.TITLE_REQUIRED);
        }
        if (learningCode == null || learningCode.isEmpty()
                || fromCode == null || fromCode.isEmpty()
                || levelCode == null || levelCode.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }

        Language learning = languageRepo.findById(learningCode).orElseThrow(ApiException::notFound);
        Language from = languageRepo.findById(fromCode).orElseThrow(ApiException::notFound);
        ProficiencyLevel lvl = levelRepo.findById(levelCode).orElseThrow(ApiException::notFound);

        Course saved = courseRepo.save(
                Course.builder()
                        .learningLanguage(learning)
                        .fromLanguage(from)
                        .title(title)
                        .description(trim(req.getDescription()))
                        .proficiencyLevel(lvl)
                        .countryIconPath(trim(req.getCountryIconPath()))
                        .build()
        );
        return toDto(saved);
    }

    @Override
    public Optional<CourseDto> findById(Long id) {
        return courseRepo.findById(id).map(this::toDto);
    }

    @Override
    public List<CourseDto> findAll() {
        return courseRepo.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public List<CourseDto> findBy(String learningLanguageCode, String fromLanguageCode, String levelCode) {
        String lang = (learningLanguageCode == null || learningLanguageCode.isBlank()) ? null : learningLanguageCode.trim();
        String src = (fromLanguageCode == null || fromLanguageCode.isBlank()) ? null : fromLanguageCode.trim();
        String lvl = (levelCode == null || levelCode.isBlank()) ? null : levelCode.trim();
        return courseRepo.findByFilters(lang, src, lvl).stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public CourseDto update(Long id, UpdateCourseRequest req) {
        SecurityUtils.requireAdmin(auth());

        Course c = courseRepo.findById(id).orElseThrow(ApiException::notFound);
        if (req.getLearningLanguageCode() != null) {
            String code = trim(req.getLearningLanguageCode());
            c.setLearningLanguage(languageRepo.findById(code).orElseThrow(ApiException::notFound));
        }
        if (req.getFromLanguageCode() != null) {
            String code = trim(req.getFromLanguageCode());
            c.setFromLanguage(languageRepo.findById(code).orElseThrow(ApiException::notFound));
        }
        if (req.getLevelCode() != null) {
            String code = trim(req.getLevelCode());
            c.setProficiencyLevel(levelRepo.findById(code).orElseThrow(ApiException::notFound));
        }
        if (req.getTitle() != null) {
            String t = trim(req.getTitle());
            if (t == null || t.isEmpty()) {
                throw ApiException.badRequest(ErrorCode.TITLE_REQUIRED);
            }
            c.setTitle(t);
        }
        if (req.getDescription() != null) c.setDescription(trim(req.getDescription()));
        if (req.getCountryIconPath() != null) c.setCountryIconPath(trim(req.getCountryIconPath()));
        return toDto(courseRepo.save(c));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        SecurityUtils.requireAdmin(auth());

        Course c = courseRepo.findById(id)
                .orElseThrow(ApiException::notFound);
        courseRepo.delete(c);
    }

    @Transactional
    @Override
    public CourseDto updateCountryIconPath(Long id, String countryIconPath) {
        SecurityUtils.requireAdmin(auth());

        Course c = courseRepo.findById(id).orElseThrow(ApiException::notFound);
        c.setCountryIconPath(countryIconPath);
        return toDto(courseRepo.save(c));
    }

    private CourseDto toDto(Course c) {
        return CourseDto.builder()
                .id(c.getId())
                .learningLanguageCode(c.getLearningLanguage() != null ? c.getLearningLanguage().getCode() : null)
                .fromLanguageCode(c.getFromLanguage() != null ? c.getFromLanguage().getCode() : null)
                .title(c.getTitle())
                .description(c.getDescription())
                .levelCode(c.getProficiencyLevel() != null ? c.getProficiencyLevel().getCode() : null)
                .countryIconPath(c.getCountryIconPath())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
