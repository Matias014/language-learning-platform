package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.exerciseOption.CreateExerciseOptionRequest;
import com.languageschool.backend.dto.exerciseOption.ExerciseOptionDto;
import com.languageschool.backend.dto.exerciseOption.UpdateExerciseOptionRequest;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.util.SecurityUtils;
import com.languageschool.backend.service.ExerciseOptionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ExerciseOptionServiceImpl implements ExerciseOptionService {

    private final ExerciseOptionRepository repo;
    private final ExerciseRepository exerciseRepo;
    private final UserRepository userRepo;
    private final CourseEnrollmentRepository enrollRepo;

    public ExerciseOptionServiceImpl(ExerciseOptionRepository repo,
                                     ExerciseRepository exerciseRepo,
                                     UserRepository userRepo,
                                     CourseEnrollmentRepository enrollRepo) {
        this.repo = repo;
        this.exerciseRepo = exerciseRepo;
        this.userRepo = userRepo;
        this.enrollRepo = enrollRepo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return SecurityUtils.isAdmin(a);
    }

    private Long meIdOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null) {
            return null;
        }
        return userRepo.findByLogin(a.getName()).map(u -> u.getId()).orElse(null);
    }

    private void ensureEnrolledOrAdmin(Long courseId) {
        if (isAdmin()) {
            return;
        }
        Long meId = meIdOrNull();
        if (meId == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        boolean ok = enrollRepo.existsByUser_IdAndCourse_Id(meId, courseId);
        if (!ok) {
            throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
        }
    }

    @Transactional
    @Override
    public ExerciseOptionDto create(CreateExerciseOptionRequest req) {
        SecurityUtils.requireAdmin(auth());

        Exercise ex = exerciseRepo.findById(req.getExerciseId())
                .orElseThrow(ApiException::notFound);
        if (ex.getType() != ExerciseType.quiz) {
            throw ApiException.badRequest(ErrorCode.NOT_A_QUIZ_EXERCISE);
        }

        String content = trim(req.getContent());
        if (content == null || content.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.CONTENT_REQUIRED);
        }
        if (req.getOrderNumber() == null || req.getOrderNumber() < 1) {
            throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
        }
        if (repo.existsByExercise_IdAndOrderNumberAndDeletedAtIsNull(ex.getId(), req.getOrderNumber())) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }

        ExerciseOption opt = new ExerciseOption();
        opt.setExercise(ex);
        opt.setContent(content);
        opt.setOrderNumber(req.getOrderNumber());

        try {
            return toDto(repo.save(opt));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Override
    public Optional<ExerciseOptionDto> findById(Long id) {
        return repo.findById(id).map(opt -> {
            Exercise ex = opt.getExercise();
            if (ex != null && ex.getLesson() != null && ex.getLesson().getCourse() != null) {
                Long courseId = ex.getLesson().getCourse().getId();
                if (courseId != null) {
                    ensureEnrolledOrAdmin(courseId);
                }
            }
            return toDto(opt);
        });
    }

    @Override
    public List<ExerciseOptionDto> findByExercise(Long exerciseId) {
        Exercise ex = exerciseRepo.findById(exerciseId)
                .orElseThrow(ApiException::notFound);
        if (ex.getLesson() != null && ex.getLesson().getCourse() != null) {
            Long courseId = ex.getLesson().getCourse().getId();
            if (courseId != null) {
                ensureEnrolledOrAdmin(courseId);
            }
        }
        return repo.findByExercise_IdOrderByOrderNumberAsc(exerciseId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ExerciseOptionDto update(Long id, UpdateExerciseOptionRequest req) {
        SecurityUtils.requireAdmin(auth());

        ExerciseOption opt = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        Exercise ex = opt.getExercise();

        if (ex.getType() != ExerciseType.quiz) {
            throw ApiException.badRequest(ErrorCode.NOT_A_QUIZ_EXERCISE);
        }

        if (req.getContent() != null) {
            String c = trim(req.getContent());
            if (c == null || c.isEmpty()) {
                throw ApiException.badRequest(ErrorCode.CONTENT_REQUIRED);
            }
            opt.setContent(c);
        }

        if (req.getOrderNumber() != null) {
            if (req.getOrderNumber() < 1) {
                throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
            }
            if (!req.getOrderNumber().equals(opt.getOrderNumber())
                    && repo.existsByExercise_IdAndOrderNumberAndDeletedAtIsNull(ex.getId(), req.getOrderNumber())) {
                throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
            }
            opt.setOrderNumber(req.getOrderNumber());
        }

        try {
            return toDto(repo.save(opt));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        SecurityUtils.requireAdmin(auth());

        ExerciseOption opt = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        Exercise ex = opt.getExercise();
        if (ex != null && ex.getCorrectOption() != null
                && ex.getCorrectOption().getId().equals(opt.getId())) {
            ex.setCorrectOption(null);
            exerciseRepo.save(ex);
        }
        repo.delete(opt);
    }

    private ExerciseOptionDto toDto(ExerciseOption e) {
        Long exerciseId = e.getExercise() != null ? e.getExercise().getId() : null;
        return ExerciseOptionDto.builder()
                .id(e.getId())
                .exerciseId(exerciseId)
                .content(e.getContent())
                .orderNumber(e.getOrderNumber())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
