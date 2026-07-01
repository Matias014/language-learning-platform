package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.exercise.CreateExerciseRequest;
import com.languageschool.backend.dto.exercise.ExerciseDto;
import com.languageschool.backend.dto.exercise.UpdateExerciseRequest;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.ExerciseService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository repo;
    private final LessonRepository lessonRepo;
    private final ExerciseOptionRepository optionRepo;
    private final UserRepository userRepo;
    private final CourseEnrollmentRepository enrollRepo;

    public ExerciseServiceImpl(ExerciseRepository repo,
                               LessonRepository lessonRepo,
                               ExerciseOptionRepository optionRepo,
                               UserRepository userRepo,
                               CourseEnrollmentRepository enrollRepo) {
        this.repo = repo;
        this.lessonRepo = lessonRepo;
        this.optionRepo = optionRepo;
        this.userRepo = userRepo;
        this.enrollRepo = enrollRepo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static void requireNonEmpty(String v, ErrorCode code) {
        if (v == null || v.isBlank()) {
            throw ApiException.badRequest(code);
        }
    }

    private static void validateFillInSchema(Map<String, Object> schema) {
        if (schema == null) return;
        Object acceptable = schema.get("acceptable");
        if (acceptable != null && !(acceptable instanceof java.util.List<?>)) {
            throw ApiException.badRequest(ErrorCode.INVALID_ANSWER_SCHEMA_ACCEPTABLE);
        }
    }

    private static void cleanupCorrectOptionIfSoftDeleted(Exercise e) {
        try {
            ExerciseOption co = e.getCorrectOption();
            if (co != null && co.getDeletedAt() != null) {
                e.setCorrectOption(null);
            }
        } catch (EntityNotFoundException ignore) {
            e.setCorrectOption(null);
        }
    }

    private boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return SecurityUtils.isAdmin(a);
    }

    private Long meIdOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null) return null;
        return userRepo.findByLogin(a.getName()).map(User::getId).orElse(null);
    }

    private void ensureEnrolledOrAdmin(Long courseId) {
        if (isAdmin()) return;
        Long meId = meIdOrNull();
        if (meId == null) throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        boolean ok = enrollRepo.existsByUser_IdAndCourse_Id(meId, courseId);
        if (!ok) throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
    }

    @Transactional
    @Override
    public ExerciseDto create(CreateExerciseRequest req) {
        Lesson lesson = lessonRepo.findById(req.getLessonId()).orElseThrow(ApiException::notFound);
        if (!isAdmin()) throw ApiException.forbidden();

        requireNonEmpty(req.getQuestion(), ErrorCode.QUESTION_REQUIRED);
        if (req.getType() == null) throw ApiException.badRequest(ErrorCode.TYPE_REQUIRED);
        if (req.getDifficulty() == null) throw ApiException.badRequest(ErrorCode.DIFFICULTY_REQUIRED);
        if (req.getOrderNumber() == null) throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_REQUIRED);
        if (req.getOrderNumber() < 1) throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
        if (req.getType() == ExerciseType.fill_in) validateFillInSchema(req.getAnswerSchema());
        if (req.getType() != ExerciseType.writing && req.getType() != ExerciseType.fill_in && req.getPassingScore() != null) {
            throw ApiException.badRequest(ErrorCode.PASSING_SCORE_NOT_ALLOWED);
        }
        if (req.getXp() != null && req.getXp() < 0) {
            throw ApiException.badRequest(ErrorCode.NEGATIVE_XP);
        }

        Exercise e = new Exercise();
        e.setLesson(lesson);
        e.setType(req.getType());
        e.setQuestion(trim(req.getQuestion()));
        if (req.getType() == ExerciseType.fill_in) {
            e.setAnswerSchema(req.getAnswerSchema());
        } else {
            e.setAnswerSchema(null);
        }
        e.setSampleAnswer(trim(req.getSampleAnswer()));
        e.setDifficulty(req.getDifficulty());
        e.setXp(req.getXp() == null ? 0 : req.getXp());
        e.setOrderNumber(req.getOrderNumber());
        if (req.getPassingScore() != null && (req.getType() == ExerciseType.writing || req.getType() == ExerciseType.fill_in)) {
            BigDecimal ps = req.getPassingScore();
            if (ps.compareTo(BigDecimal.ZERO) < 0 || ps.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.UNPROCESSABLE_ENTITY);
            }
            e.setPassingScore(ps);
        } else {
            e.setPassingScore(null);
        }

        try {
            return toDto(repo.save(e));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Override
    public Optional<ExerciseDto> findById(Long id) {
        return repo.findById(id).map(e -> {
            Lesson lesson = e.getLesson();
            if (lesson != null && lesson.getCourse() != null && lesson.getCourse().getId() != null) {
                ensureEnrolledOrAdmin(lesson.getCourse().getId());
            }
            return toDto(e);
        });
    }

    @Override
    public List<ExerciseDto> findByLesson(Long lessonId) {
        Lesson l = lessonRepo.findById(lessonId).orElseThrow(ApiException::notFound);
        ensureEnrolledOrAdmin(l.getCourse().getId());
        return repo.findByLesson_Id(lessonId).stream()
                .sorted(Comparator.comparing(Exercise::getOrderNumber))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ExerciseDto update(Long id, UpdateExerciseRequest req) {
        Exercise e = repo.findById(id).orElseThrow(ApiException::notFound);
        if (!isAdmin()) throw ApiException.forbidden();

        if (req.getQuestion() != null) {
            String q = trim(req.getQuestion());
            requireNonEmpty(q, ErrorCode.QUESTION_REQUIRED);
            e.setQuestion(q);
        }
        if (req.getAnswerSchema() != null && e.getType() == ExerciseType.fill_in) {
            validateFillInSchema(req.getAnswerSchema());
            e.setAnswerSchema(req.getAnswerSchema());
        }
        if (req.getSampleAnswer() != null) e.setSampleAnswer(trim(req.getSampleAnswer()));
        if (req.getDifficulty() != null) e.setDifficulty(req.getDifficulty());
        if (req.getXp() != null) {
            if (req.getXp() < 0) {
                throw ApiException.badRequest(ErrorCode.NEGATIVE_XP);
            }
            e.setXp(req.getXp());
        }
        if (req.getOrderNumber() != null) {
            if (req.getOrderNumber() < 1) {
                throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
            }
            e.setOrderNumber(req.getOrderNumber());
        }

        if (req.getPassingScore() != null) {
            if (e.getType() != ExerciseType.writing && e.getType() != ExerciseType.fill_in) {
                throw ApiException.badRequest(ErrorCode.PASSING_SCORE_NOT_ALLOWED);
            }
            BigDecimal ps = req.getPassingScore();
            if (ps.compareTo(BigDecimal.ZERO) < 0 || ps.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.UNPROCESSABLE_ENTITY);
            }
            e.setPassingScore(ps);
        }

        if (req.getCorrectOptionId() != null) {
            if (e.getType() != ExerciseType.quiz) {
                throw ApiException.badRequest(ErrorCode.CANNOT_SET_CORRECT_OPTION_FOR_NON_QUIZ);
            }
            ExerciseOption opt = optionRepo.findById(req.getCorrectOptionId()).orElseThrow(ApiException::notFound);
            if (opt.getExercise() == null || !opt.getExercise().getId().equals(e.getId())) {
                throw ApiException.badRequest(ErrorCode.CORRECT_OPTION_NOT_BELONG_TO_EXERCISE);
            }
            e.setCorrectOption(opt);
        }

        if (e.getType() != ExerciseType.fill_in) {
            e.setAnswerSchema(null);
        }

        cleanupCorrectOptionIfSoftDeleted(e);
        try {
            return toDto(repo.save(e));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Exercise e = repo.findById(id).orElseThrow(ApiException::notFound);
        if (!isAdmin()) throw ApiException.forbidden();
        repo.delete(e);
    }

    private ExerciseDto toDto(Exercise e) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = SecurityUtils.isAdmin(auth);
        Long correctOptionId = admin && e.getCorrectOption() != null ? e.getCorrectOption().getId() : null;
        Map<String, Object> answerSchema = admin ? e.getAnswerSchema() : null;
        String sampleAnswer = admin ? e.getSampleAnswer() : null;

        return ExerciseDto.builder()
                .id(e.getId())
                .lessonId(e.getLesson().getId())
                .type(e.getType())
                .question(e.getQuestion())
                .answerSchema(answerSchema)
                .sampleAnswer(sampleAnswer)
                .difficulty(e.getDifficulty())
                .xp(e.getXp())
                .orderNumber(e.getOrderNumber())
                .correctOptionId(correctOptionId)
                .passingScore(e.getPassingScore())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
