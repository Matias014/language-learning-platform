package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.lesson.CreateLessonRequest;
import com.languageschool.backend.dto.lesson.LessonDto;
import com.languageschool.backend.dto.lesson.UpdateLessonRequest;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.service.LessonService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private final LessonRepository repo;
    private final CourseRepository courseRepo;

    public LessonServiceImpl(LessonRepository repo, CourseRepository courseRepo) {
        this.repo = repo;
        this.courseRepo = courseRepo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Transactional
    @Override
    public LessonDto create(CreateLessonRequest req) {
        SecurityUtils.requireAdmin(auth());
        Course c = courseRepo.findById(req.getCourseId()).orElseThrow(ApiException::notFound);
        String title = trim(req.getTitle());
        if (title == null || title.isEmpty()) throw ApiException.badRequest(ErrorCode.TITLE_REQUIRED);
        if (req.getOrderNumber() == null) throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_REQUIRED);
        if (req.getOrderNumber() < 1) throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
        Lesson l = Lesson.builder()
                .course(c)
                .title(title)
                .description(trim(req.getDescription()))
                .orderNumber(req.getOrderNumber())
                .build();
        try {
            return toDto(repo.save(l));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Override
    public Optional<LessonDto> findById(Long id) {
        return repo.findById(id).map(this::toDto);
    }

    @Override
    public List<LessonDto> findByCourse(Long courseId) {
        return repo.findByCourse_IdOrderByOrderNumberAsc(courseId).stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public LessonDto update(Long id, UpdateLessonRequest req) {
        SecurityUtils.requireAdmin(auth());
        Lesson l = repo.findById(id).orElseThrow(ApiException::notFound);
        if (req.getTitle() != null) {
            String title = trim(req.getTitle());
            if (title.isEmpty()) throw ApiException.badRequest(ErrorCode.TITLE_REQUIRED);
            l.setTitle(title);
        }
        if (req.getDescription() != null) {
            l.setDescription(trim(req.getDescription()));
        }
        if (req.getOrderNumber() != null) {
            if (req.getOrderNumber() < 1) throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
            l.setOrderNumber(req.getOrderNumber());
        }
        try {
            return toDto(repo.save(l));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        SecurityUtils.requireAdmin(auth());
        Lesson l = repo.findById(id).orElseThrow(ApiException::notFound);
        repo.delete(l);
    }

    private LessonDto toDto(Lesson l) {
        return LessonDto.builder()
                .id(l.getId())
                .courseId(l.getCourse().getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .orderNumber(l.getOrderNumber())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
