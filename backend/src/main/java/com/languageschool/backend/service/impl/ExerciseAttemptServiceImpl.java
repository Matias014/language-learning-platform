package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.exerciseAttempt.CreateExerciseAttemptRequest;
import com.languageschool.backend.dto.exerciseAttempt.ExerciseAttemptDto;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseAttempt;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.ExerciseAttemptRepository;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.CourseProgressService;
import com.languageschool.backend.service.ExerciseAttemptService;
import com.languageschool.backend.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.languageschool.backend.util.SecurityUtils.ensureOwnerOrAdmin;
import static com.languageschool.backend.util.SecurityUtils.isAdmin;

@Service
@Transactional(readOnly = true)
public class ExerciseAttemptServiceImpl implements ExerciseAttemptService {

    private final ExerciseAttemptRepository repo;
    private final UserRepository userRepo;
    private final ExerciseRepository exerciseRepo;
    private final ExerciseOptionRepository optionRepo;
    private final CourseProgressService courseProgress;
    private final UserService userService;

    public ExerciseAttemptServiceImpl(ExerciseAttemptRepository repo,
                                      UserRepository userRepo,
                                      ExerciseRepository exerciseRepo,
                                      ExerciseOptionRepository optionRepo,
                                      CourseProgressService courseProgress,
                                      UserService userService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.exerciseRepo = exerciseRepo;
        this.optionRepo = optionRepo;
        this.courseProgress = courseProgress;
        this.userService = userService;
    }

    @Override
    public List<ExerciseAttemptDto> findAll() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw ApiException.forbidden();
        }
        return repo.findAll(Sort.by(Sort.Direction.DESC, "submittedAt")).stream()
                .map(this::toDto)
                .toList();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    @Transactional
    @Override
    public ExerciseAttemptDto create(Long userId, CreateExerciseAttemptRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        User u = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(userService, auth, u.getId());

        Exercise e = exerciseRepo.findById(req.getExerciseId())
                .orElseThrow(ApiException::notFound);

        int nextAttempt = repo.findTopByUser_IdAndExercise_IdOrderByAttemptNumberDesc(u.getId(), e.getId())
                .map(a -> a.getAttemptNumber() + 1)
                .orElse(1);

        ExerciseAttempt a = new ExerciseAttempt();
        a.setUser(u);
        a.setExercise(e);
        a.setAttemptNumber(nextAttempt);
        Integer dur = req.getDurationSeconds();
        a.setDurationSeconds(dur == null ? null : Math.max(0, dur));

        if (e.getType() == ExerciseType.quiz) {
            if (req.getChosenOptionId() == null) {
                throw ApiException.badRequest(ErrorCode.CHOSEN_OPTION_REQUIRED_FOR_QUIZ);
            }
            ExerciseOption chosen = optionRepo.findById(req.getChosenOptionId())
                    .orElseThrow(ApiException::notFound);
            if (chosen.getExercise() == null || !chosen.getExercise().getId().equals(e.getId())) {
                throw ApiException.badRequest(ErrorCode.CHOSEN_OPTION_NOT_BELONG_TO_EXERCISE);
            }

            a.setChosenOption(chosen);
            a.setSubmittedAnswer(null);
            boolean correct = e.getCorrectOption() != null
                    && e.getCorrectOption().getId().equals(chosen.getId());
            a.setCorrect(correct);
            a.setScore(correct ? new BigDecimal("100.00") : new BigDecimal("0.00"));
        } else {
            if (req.getChosenOptionId() != null) {
                throw ApiException.badRequest(ErrorCode.OPTION_NOT_ALLOWED_FOR_NON_QUIZ);
            }
            String answer = trim(req.getSubmittedAnswer());
            if (answer == null || answer.isEmpty()) {
                throw ApiException.badRequest(ErrorCode.ANSWER_REQUIRED_FOR_NON_QUIZ);
            }
            a.setChosenOption(null);
            a.setSubmittedAnswer(answer);
            a.setCorrect(false);
            a.setScore(null);
        }

        try {
            a = repo.save(a);
        } catch (DataIntegrityViolationException ex) {
            int retryAttempt = repo.findTopByUser_IdAndExercise_IdOrderByAttemptNumberDesc(u.getId(), e.getId())
                    .map(x -> x.getAttemptNumber() + 1)
                    .orElse(1);
            a.setAttemptNumber(retryAttempt);
            try {
                a = repo.save(a);
            } catch (DataIntegrityViolationException ex2) {
                throw ApiException.conflict(ErrorCode.CONCURRENT_ATTEMPT);
            }
        }

        courseProgress.touchLastActivity(u.getId(), e.getLesson().getCourse().getId());
        return toDto(a);
    }

    @Override
    public ExerciseAttemptDto getById(Long id) {
        ExerciseAttempt a = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long ownerId = a.getUser() != null ? a.getUser().getId() : null;
        ensureOwnerOrAdmin(userService, auth, ownerId);
        return toDto(a);
    }

    @Override
    public List<ExerciseAttemptDto> findByUser(Long userId) {
        Long ownerId = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getId();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        ensureOwnerOrAdmin(userService, auth, ownerId);
        return repo.findByUser_IdOrderBySubmittedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public List<ExerciseAttemptDto> findByExercise(Long exerciseId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw ApiException.forbidden();
        }
        return repo.findByExercise_IdOrderBySubmittedAtDesc(exerciseId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public List<ExerciseAttemptDto> findByUserAndExercise(Long userId, Long exerciseId) {
        Long ownerId = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getId();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        ensureOwnerOrAdmin(userService, auth, ownerId);
        return repo.findByUser_IdAndExercise_IdOrderBySubmittedAtDesc(userId, exerciseId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public Optional<ExerciseAttemptDto> findLatestByUserAndExercise(Long userId, Long exerciseId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        ensureOwnerOrAdmin(userService, auth, userId);
        return repo.findTopByUser_IdAndExercise_IdOrderByAttemptNumberDesc(userId, exerciseId)
                .map(this::toDto);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        ExerciseAttempt a = repo.findById(id)
                .orElseThrow(ApiException::notFound);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw ApiException.forbidden();
        }

        repo.delete(a);
    }

    private ExerciseAttemptDto toDto(ExerciseAttempt a) {
        return ExerciseAttemptDto.builder()
                .id(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .exerciseId(a.getExercise() != null ? a.getExercise().getId() : null)
                .submittedAnswer(a.getSubmittedAnswer())
                .chosenOptionId(a.getChosenOption() != null ? a.getChosenOption().getId() : null)
                .correct(a.isCorrect())
                .score(a.getScore())
                .feedback(a.getFeedback())
                .attemptNumber(a.getAttemptNumber())
                .submittedAt(a.getSubmittedAt())
                .durationSeconds(a.getDurationSeconds())
                .build();
    }
}
