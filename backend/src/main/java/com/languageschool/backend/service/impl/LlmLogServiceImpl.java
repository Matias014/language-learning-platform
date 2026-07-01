package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.llmLog.LlmLogDto;
import com.languageschool.backend.entity.ChatSession;
import com.languageschool.backend.entity.ExerciseAttempt;
import com.languageschool.backend.entity.LlmLog;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.ChatSessionRepository;
import com.languageschool.backend.repository.ExerciseAttemptRepository;
import com.languageschool.backend.repository.LlmLogRepository;
import com.languageschool.backend.service.LlmLogService;
import com.languageschool.backend.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.languageschool.backend.util.SecurityUtils.ensureOwnerOrAdmin;
import static com.languageschool.backend.util.SecurityUtils.requireAdmin;

@Service
@Transactional
public class LlmLogServiceImpl implements LlmLogService {

    private final LlmLogRepository repo;
    private final ExerciseAttemptRepository attemptRepo;
    private final ChatSessionRepository sessionRepo;
    private final UserService userService;
    private final int retentionDays;

    @PersistenceContext
    private EntityManager em;

    public LlmLogServiceImpl(LlmLogRepository repo,
                             ExerciseAttemptRepository attemptRepo,
                             ChatSessionRepository sessionRepo,
                             UserService userService,
                             @Value("${ai.llm.logs.retention-days:30}") int retentionDays) {
        this.userService = userService;
        this.repo = repo;
        this.attemptRepo = attemptRepo;
        this.sessionRepo = sessionRepo;
        this.retentionDays = retentionDays;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LlmLogDto> findById(Long id) {
        return repo.findById(id).map(log -> {
            Long ownerId = computeOwnerId(log);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (ownerId != null) {
                ensureOwnerOrAdmin(userService, auth, ownerId);
            } else {
                requireAdmin(auth);
            }
            return toDto(log);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public LlmLogDto getSecured(Long id) {
        LlmLog log = repo.findById(id).orElseThrow(ApiException::notFound);
        Long ownerId = computeOwnerId(log);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ownerId != null) {
            ensureOwnerOrAdmin(userService, auth, ownerId);
        } else {
            requireAdmin(auth);
        }
        return toDto(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LlmLogDto> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        requireAdmin(auth);
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LlmLogDto> findByUser(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ensureOwnerOrAdmin(userService, auth, userId);

        List<LlmLog> direct = repo.findByUser_IdOrderByCreatedAtDesc(userId);
        List<LlmLog> bySess = repo.findByChatSession_User_IdOrderByCreatedAtDesc(userId);
        List<LlmLog> byAtt = em.createQuery(
                        "select l from LlmLog l where l.exerciseAttempt.user.id=:uid order by l.createdAt desc",
                        LlmLog.class)
                .setParameter("uid", userId)
                .getResultList();

        return java.util.stream.Stream.of(direct, byAtt, bySess)
                .flatMap(java.util.Collection::stream)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(LlmLog::getId, x -> x, (a, b) -> a),
                        m -> m.values().stream()
                                .sorted(Comparator.comparing(LlmLog::getCreatedAt).reversed())
                                .map(this::toDto)
                                .toList()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LlmLogDto> findByUserSecured(Long userId) {
        return findByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LlmLogDto> findByLesson(Long lessonId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        requireAdmin(auth);
        return repo.findByLesson_IdOrderByCreatedAtDesc(lessonId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LlmLogDto> findByExerciseAttempt(Long attemptId) {
        ExerciseAttempt attempt = attemptRepo.findById(attemptId).orElseThrow(ApiException::notFound);
        Long ownerId = attempt.getUser() != null ? attempt.getUser().getId() : null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ownerId != null) {
            ensureOwnerOrAdmin(userService, auth, ownerId);
        } else {
            requireAdmin(auth);
        }
        return repo.findByExerciseAttempt_IdOrderByCreatedAtDesc(attemptId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LlmLogDto> findByChatSession(Long sessionId) {
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow(ApiException::notFound);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long ownerId = session.getUser() != null ? session.getUser().getId() : null;
        if (ownerId != null) {
            ensureOwnerOrAdmin(userService, auth, ownerId);
        } else {
            requireAdmin(auth);
        }
        return repo.findByChatSession_IdOrderByCreatedAtDesc(sessionId).stream().map(this::toDto).toList();
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Warsaw")
    public void purgeOldLogs() {
        Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        repo.deleteByCreatedAtBefore(threshold);
    }

    private Long computeOwnerId(LlmLog l) {
        if (l.getUser() != null) return l.getUser().getId();
        if (l.getExerciseAttempt() != null && l.getExerciseAttempt().getUser() != null)
            return l.getExerciseAttempt().getUser().getId();
        if (l.getChatSession() != null && l.getChatSession().getUser() != null)
            return l.getChatSession().getUser().getId();
        return null;
    }

    private LlmLogDto toDto(LlmLog e) {
        Long userId = e.getUser() != null ? e.getUser().getId() : null;
        Long lessonId = e.getLesson() != null ? e.getLesson().getId() : null;
        Long attemptId = e.getExerciseAttempt() != null ? e.getExerciseAttempt().getId() : null;
        Long sessionId = e.getChatSession() != null ? e.getChatSession().getId() : null;
        return LlmLogDto.builder()
                .id(e.getId())
                .userId(userId)
                .lessonId(lessonId)
                .exerciseAttemptId(attemptId)
                .chatSessionId(sessionId)
                .interactionType(e.getInteractionType())
                .model(e.getModel())
                .tokensIn(e.getTokensIn())
                .tokensOut(e.getTokensOut())
                .latencyMs(e.getLatencyMs())
                .params(e.getParams())
                .status(e.getStatus())
                .prompt(e.getPrompt())
                .response(e.getResponse())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
