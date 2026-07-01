package com.languageschool.backend.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.config.AiRuntimeLimits;
import com.languageschool.backend.dto.ai.GradeResponse;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseAttempt;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LlmLog;
import com.languageschool.backend.entity.LlmStatus;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.ExerciseAttemptRepository;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.LlmLogRepository;
import com.languageschool.backend.service.AiGradingService;
import com.languageschool.backend.service.CourseProgressService;
import com.languageschool.backend.service.ExerciseAwardService;
import com.languageschool.backend.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.languageschool.backend.service.ai.LlmSupport.formatUsageSuffix;
import static com.languageschool.backend.service.ai.LlmSupport.mapStatus;
import static com.languageschool.backend.service.ai.LlmSupport.redact;
import static com.languageschool.backend.service.ai.LlmSupport.truncate;

@Service
@ConditionalOnBean(ClaudeClient.class)
public class AiGradingServiceImpl implements AiGradingService {

    private static final Logger log = LoggerFactory.getLogger(AiGradingServiceImpl.class);

    private final ClaudeClient claude;
    private final ExerciseAttemptRepository attemptRepo;
    private final ExerciseOptionRepository optionRepo;
    private final LlmLogRepository logRepo;
    private final CourseProgressService courseProgress;
    private final AiRuntimeLimits limits;
    private final ExerciseAwardService awards;
    private final AiProps props;
    private final CourseEnrollmentRepository enrollmentRepo;
    private final ObjectMapper om = new ObjectMapper();

    public AiGradingServiceImpl(ClaudeClient claude,
                                ExerciseAttemptRepository attemptRepo,
                                ExerciseOptionRepository optionRepo,
                                LlmLogRepository logRepo,
                                CourseProgressService courseProgress,
                                AiRuntimeLimits limits,
                                ExerciseAwardService awards,
                                AiProps props,
                                CourseEnrollmentRepository enrollmentRepo) {
        this.claude = claude;
        this.attemptRepo = attemptRepo;
        this.optionRepo = optionRepo;
        this.logRepo = logRepo;
        this.courseProgress = courseProgress;
        this.limits = limits;
        this.awards = awards;
        this.props = props;
        this.enrollmentRepo = enrollmentRepo;
    }

    @Override
    @Transactional
    public GradeResponse gradeAttempt(Long attemptId, String login) {
        ExerciseAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(ApiException::notFound);

        User user = attempt.getUser();
        Exercise ex = attempt.getExercise();
        String ownerLogin = user != null ? user.getLogin() : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        boolean isAdmin = SecurityUtils.isAdmin(auth);
        if (!isAdmin && login != null && !login.isBlank() && ownerLogin != null && !ownerLogin.equals(login)) {
            throw ApiException.forbidden();
        }

        if (ex.getType() == ExerciseType.quiz) {
            return gradeQuizAttempt(attempt, ex, user);
        }

        return gradeFreeFormAttempt(attempt, ex, user);
    }

    private GradeResponse gradeQuizAttempt(ExerciseAttempt attempt, Exercise ex, User user) {
        if (attempt.getChosenOption() == null) {
            throw ApiException.badRequest(ErrorCode.CHOSEN_OPTION_REQUIRED_FOR_QUIZ);
        }

        ExerciseOption chosen = optionRepo.findById(attempt.getChosenOption().getId())
                .orElseThrow(ApiException::notFound);

        if (chosen.getExercise() == null || !chosen.getExercise().getId().equals(ex.getId())) {
            throw ApiException.badRequest(ErrorCode.CHOSEN_OPTION_NOT_BELONG_TO_EXERCISE);
        }

        Long correctId = ex.getCorrectOption() != null ? ex.getCorrectOption().getId() : null;
        boolean isCorrect = correctId != null && correctId.equals(chosen.getId());

        BigDecimal score = BigDecimal
                .valueOf(isCorrect ? 100.0 : 0.0)
                .setScale(2, RoundingMode.HALF_UP);

        String fromLanguageCode = resolveSupportLanguageCode(ex);
        String sys = """
                You are a grading assistant for a language-learning app. You are evaluating a WRITTEN multiple-choice exercise, not a real situation. There is no audio, image, or live scene. Use only the text included in this request.
                If the request is outside language learning, output {"correct":false,"feedback":"","hints":[],"score":0}.
                Refuse sexual, explicit, hateful, violent, illegal or personal-data requests and output the same empty structure.
                Reply ONLY strict JSON: {"correct":true|false,"feedback":"string","hints":["string","..."],"score":0-100}
                Write feedback and hints in: %s.
                For multiple-choice questions, never reveal the exact correct option or its text and do not list concrete options in the output.
                Give the true main reason for the error. If grammar is fine and the student's tense/aspect matches the correct option, do not mention tense/aspect; focus on meaning, context or task intent.
                Hints must be 2–4 short, language-focused tips; never include UI actions like "listen", "look", "watch", "analyze the scene", "observe the picture/recording", "click", and never refer to pictures, scenes, or audio.
                """.formatted(fromLanguageCode);

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("Exercise exerciseType: quiz").append("\n");
        userMsg.append("Context: text-only multiple-choice exercise from a language-learning app; there is no picture or audio. Do not imagine any real classroom or scene.").append("\n");
        userMsg.append("Question: ").append(ex.getQuestion()).append("\n");
        List<ExerciseOption> opts = optionRepo.findByExercise_IdOrderByOrderNumberAsc(ex.getId());
        if (opts != null && !opts.isEmpty()) {
            StringBuilder optList = new StringBuilder();
            for (int i = 0; i < opts.size(); i++) {
                ExerciseOption o = opts.get(i);
                optList.append((char) ('A' + i)).append(") ").append(o.getContent());
                if (i + 1 < opts.size()) optList.append(" | ");
            }
            userMsg.append("Options (for context only; do not reveal them back): ").append(optList).append("\n");
        }
        userMsg.append("Student chosen option text: ").append(chosen.getContent() == null ? "" : chosen.getContent().trim()).append("\n");
        if (ex.getCorrectOption() != null) {
            userMsg.append("Correct option text (for checking only; do not reveal to student): ").append(ex.getCorrectOption().getContent()).append("\n");
        }
        userMsg.append("Meta: if the tense/aspect of the student's option and the correct option are the same, do not mention tense/aspect in feedback.");

        List<Map<String, Object>> messages = List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "text", "text", userMsg.toString()))
        ));

        String feedbackFromLlm = defaultQuizFeedback(isCorrect, fromLanguageCode);
        List<String> hintsFromLlm = List.of();

        long t0 = System.nanoTime();
        ClaudeClient.CompletionResult result = null;
        try {
            result = claude.completeWithUsage(sys, messages, limits.maxOutputTokens());
            String raw = result.text();
            Map<String, Object> parsed = safeMapRelaxed(raw);
            String f = str(parsed.get("feedback"));
            List<String> h = extractHints(parsed);
            if (f != null && !f.trim().isEmpty()) {
                feedbackFromLlm = f.trim();
            }
            if (h != null) {
                hintsFromLlm = h;
            }

            long latencyMs = (System.nanoTime() - t0) / 1_000_000;
            LlmLog logEntry = new LlmLog();
            logEntry.setUser(user);
            logEntry.setLesson(ex.getLesson());
            logEntry.setExerciseAttempt(attempt);
            logEntry.setInteractionType(InteractionType.grading);
            String safePrompt = """
                    QUIZ GRADING
                    Question: %s
                    Chosen: %s
                    """.formatted(ex.getQuestion(), chosen.getContent());
            logEntry.setPrompt(truncate(redact(safePrompt), 4000));
            logEntry.setResponse(truncate(redact(raw), 4000) + formatUsageSuffix(result));
            logEntry.setModel(props.model());
            logEntry.setTokensIn(result.promptTokens() == null ? 0 : result.promptTokens());
            logEntry.setTokensOut(result.completionTokens() == null ? 0 : result.completionTokens());
            logEntry.setLatencyMs((int) latencyMs);
            Map<String, Object> params = new HashMap<>();
            params.put("fromLanguageCode", fromLanguageCode);
            params.put("maxOutputTokens", limits.maxOutputTokens());
            params.put("mode", "quiz_llm");
            logEntry.setParams(params);
            logEntry.setStatus(LlmStatus.ok);
            logRepo.save(logEntry);
        } catch (RestClientException exx) {
            long latencyMs = (System.nanoTime() - t0) / 1_000_000;
            LlmLog logEntry = new LlmLog();
            logEntry.setUser(user);
            logEntry.setLesson(ex.getLesson());
            logEntry.setExerciseAttempt(attempt);
            logEntry.setInteractionType(InteractionType.grading);
            String safePrompt = """
                    QUIZ GRADING
                    Question: %s
                    Chosen: %s
                    """.formatted(ex.getQuestion(), chosen.getContent());
            logEntry.setPrompt(truncate(redact(safePrompt), 4000));
            logEntry.setResponse(truncate(redact(String.valueOf(exx.getMessage())), 2000));
            logEntry.setModel(props.model());
            logEntry.setTokensIn(0);
            logEntry.setTokensOut(0);
            logEntry.setLatencyMs((int) latencyMs);
            Map<String, Object> params = new HashMap<>();
            params.put("fromLanguageCode", fromLanguageCode);
            params.put("maxOutputTokens", limits.maxOutputTokens());
            params.put("mode", "quiz_llm");
            logEntry.setParams(params);
            logEntry.setStatus(mapStatus(exx));
            logRepo.save(logEntry);
        }

        attempt.setCorrect(isCorrect);
        attempt.setScore(score);
        attempt.setFeedback(feedbackFromLlm);
        attemptRepo.save(attempt);

        touchProgressIfPossible(user, ex);

        Integer awardedXp = null;
        if (isCorrect) {
            try {
                var opt = awards.createIfEligible(attempt.getId());
                awardedXp = opt.map(d -> d.getAwardedXp()).orElse(null);
            } catch (Exception e) {
                log.debug("Award not created: {}", e.getMessage());
            }
        }

        return new GradeResponse(isCorrect, feedbackFromLlm, hintsFromLlm, attempt.getId(), awardedXp);
    }

    private GradeResponse gradeFreeFormAttempt(ExerciseAttempt attempt, Exercise ex, User user) {
        String ansRaw = attempt.getSubmittedAnswer() == null ? "" : attempt.getSubmittedAnswer().trim();
        if (ansRaw.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.ANSWER_REQUIRED_FOR_NON_QUIZ);
        }
        int maxUserChars = limits.maxUserMessageChars() <= 0 ? Integer.MAX_VALUE : limits.maxUserMessageChars();
        String ans = ansRaw.length() > maxUserChars ? ansRaw.substring(0, maxUserChars) : ansRaw;

        String fromLanguageCode = resolveSupportLanguageCode(ex);
        String sys = """
                You are a grading assistant for a language-learning app. You are evaluating a WRITTEN answer, not a real situation. There is no audio, image, or live scene. Use only the text included in this request.
                If the request is outside language learning, output {"correct":false,"feedback":"","hints":[],"score":0}.
                Refuse sexual, explicit, hateful, violent, illegal or personal-data requests and output the same empty structure.
                Reply ONLY strict JSON: {"correct":true|false,"feedback":"string","hints":["string","..."],"score":0-100}
                Write feedback and hints in: %s. Do not reveal exact answers.
                Hints must be 2–4 short, language-focused tips; never include instructions like "listen", "look", "watch", "analyze the scene", or references to pictures or audio.
                """.formatted(fromLanguageCode);

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("Exercise exerciseType: ").append(ex.getType().name().toLowerCase()).append("\n");
        userMsg.append("Context: text-only exercise from a language-learning app; there is no picture or audio. Do not imagine any real classroom or scene.").append("\n");
        userMsg.append("Question: ").append(ex.getQuestion()).append("\n");
        userMsg.append("Student answer: ").append(ans);
        if (ex.getType() == ExerciseType.fill_in && ex.getAnswerSchema() != null) {
            userMsg.append("\nAcceptable answers (do not reveal): ").append(ex.getAnswerSchema());
        } else if (ex.getType() == ExerciseType.writing && ex.getSampleAnswer() != null) {
            userMsg.append("\nReference sample answer (do not reveal): ").append(ex.getSampleAnswer());
        }

        List<Map<String, Object>> messages = List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "text", "text", userMsg.toString()))
        ));

        long t0 = System.nanoTime();
        final ClaudeClient.CompletionResult result;
        try {
            result = claude.completeWithUsage(sys, messages, limits.maxOutputTokens());
        } catch (RestClientException exx) {
            long latencyMs = (System.nanoTime() - t0) / 1_000_000;
            LlmLog logEntry = new LlmLog();
            logEntry.setUser(user);
            logEntry.setExerciseAttempt(attempt);
            logEntry.setLesson(ex.getLesson());
            logEntry.setInteractionType(InteractionType.grading);
            logEntry.setPrompt("GRADING_REQUEST");
            logEntry.setResponse(truncate(redact(String.valueOf(exx.getMessage())), 2000));
            logEntry.setModel(props.model());
            logEntry.setTokensIn(0);
            logEntry.setTokensOut(0);
            logEntry.setLatencyMs((int) latencyMs);
            Map<String, Object> params = new HashMap<>();
            params.put("fromLanguageCode", fromLanguageCode);
            params.put("maxOutputTokens", limits.maxOutputTokens());
            logEntry.setParams(params);
            logEntry.setStatus(mapStatus(exx));
            logRepo.save(logEntry);
            throw ApiException.serviceUnavailable();
        }
        long latencyMs = (System.nanoTime() - t0) / 1_000_000;

        String raw = result.text();
        Map<String, Object> parsed = safeMapRelaxed(raw);

        if (parsed.isEmpty() || (!parsed.containsKey("correct") && !parsed.containsKey("score"))) {
            LlmLog logEntry = new LlmLog();
            logEntry.setUser(user);
            logEntry.setExerciseAttempt(attempt);
            logEntry.setLesson(ex.getLesson());
            logEntry.setInteractionType(InteractionType.grading);
            logEntry.setPrompt("GRADING_REQUEST");
            logEntry.setResponse(truncate(redact(raw), 4000) + formatUsageSuffix(result));
            logEntry.setModel(props.model());
            logEntry.setTokensIn(result.promptTokens() == null ? 0 : result.promptTokens());
            logEntry.setTokensOut(result.completionTokens() == null ? 0 : result.completionTokens());
            logEntry.setLatencyMs((int) latencyMs);
            Map<String, Object> params = new HashMap<>();
            params.put("fromLanguageCode", fromLanguageCode);
            params.put("maxOutputTokens", limits.maxOutputTokens());
            params.put("parseError", true);
            logEntry.setParams(params);
            logEntry.setStatus(LlmStatus.server_error);
            logRepo.save(logEntry);
            throw ApiException.serviceUnavailable();
        }

        boolean isCorrectFromModel = parsed.get("correct") instanceof Boolean b
                ? b
                : "true".equalsIgnoreCase(String.valueOf(parsed.get("correct")));
        String feedback = str(parsed.get("feedback"));
        List<String> hints = extractHints(parsed);

        Integer scoreInt = null;
        try {
            Object sc = parsed.get("score");
            if (sc instanceof Number n) {
                scoreInt = n.intValue();
            } else if (sc != null) {
                scoreInt = Integer.parseInt(sc.toString());
            }
        } catch (Exception ignored) {
        }

        BigDecimal score = BigDecimal
                .valueOf(scoreInt == null
                        ? (isCorrectFromModel ? 100 : 0)
                        : Math.max(0, Math.min(100, scoreInt)))
                .setScale(2, RoundingMode.HALF_UP);

        boolean finalCorrect = isCorrectFromModel;
        if (ex.getPassingScore() != null) {
            finalCorrect = score.compareTo(ex.getPassingScore()) >= 0;
        }

        attempt.setCorrect(finalCorrect);
        attempt.setScore(score);
        attempt.setFeedback(feedback);
        attemptRepo.save(attempt);

        touchProgressIfPossible(user, ex);

        LlmLog logEntry = new LlmLog();
        logEntry.setUser(user);
        logEntry.setExerciseAttempt(attempt);
        logEntry.setLesson(ex.getLesson());
        logEntry.setInteractionType(InteractionType.grading);
        logEntry.setPrompt("GRADING_REQUEST");
        logEntry.setResponse(truncate(redact(raw), 4000) + formatUsageSuffix(result));
        logEntry.setModel(props.model());
        logEntry.setTokensIn(result.promptTokens() == null ? 0 : result.promptTokens());
        logEntry.setTokensOut(result.completionTokens() == null ? 0 : result.completionTokens());
        logEntry.setLatencyMs((int) latencyMs);
        Map<String, Object> params = new HashMap<>();
        params.put("fromLanguageCode", fromLanguageCode);
        params.put("maxOutputTokens", limits.maxOutputTokens());
        logEntry.setParams(params);
        logEntry.setStatus(LlmStatus.ok);
        logRepo.save(logEntry);

        Integer awardedXp = null;
        if (finalCorrect) {
            try {
                var opt = awards.createIfEligible(attempt.getId());
                awardedXp = opt.map(d -> d.getAwardedXp()).orElse(null);
            } catch (Exception e) {
                log.debug("Award not created: {}", e.getMessage());
            }
        }

        return new GradeResponse(finalCorrect, feedback, hints, attempt.getId(), awardedXp);
    }

    private Map<String, Object> safeMapRelaxed(String json) {
        Map<String, Object> v = tryParseMap(json);
        if (!v.isEmpty()) return v;
        String s = json == null ? "" : json.trim();
        s = stripCodeFence(s);
        v = tryParseMap(s);
        if (!v.isEmpty()) return v;
        String sliced = sliceFirstJsonObject(s);
        v = tryParseMap(sliced);
        if (!v.isEmpty()) return v;
        String repaired = fixTrailingCommas(sliced);
        v = tryParseMap(repaired);
        if (!v.isEmpty()) return v;
        return Map.of();
    }

    private Map<String, Object> tryParseMap(String json) {
        try {
            return om.readValue(json == null ? "{}" : json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl >= 0) t = t.substring(firstNl + 1);
        }
        if (t.endsWith("```")) {
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
        }
        return t.trim();
    }

    private String sliceFirstJsonObject(String s) {
        if (s == null) return "{}";
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return "{}";
    }

    private String fixTrailingCommas(String s) {
        if (s == null) return "{}";
        return s.replaceAll(",\\s*([}\\]])", "$1");
    }

    private List<String> extractHints(Map<String, Object> parsed) {
        try {
            Object h = parsed.get("hints");
            if (h instanceof List<?> l) {
                return l.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private String resolveSupportLanguageCode(Exercise ex) {
        try {
            if (ex.getLesson() != null
                    && ex.getLesson().getCourse() != null
                    && ex.getLesson().getCourse().getFromLanguage() != null) {
                String code = ex.getLesson().getCourse().getFromLanguage().getCode();
                return normalizeLang(code);
            }
        } catch (Exception ignored) {
        }
        return "en";
    }

    private String normalizeLang(String code) {
        if (code == null) {
            return "en";
        }
        String c = code.trim().toLowerCase(Locale.ROOT);
        if (c.isBlank()) {
            return "en";
        }
        int dash = c.indexOf('-');
        String head = dash > 0 ? c.substring(0, dash) : c;
        if (head.length() >= 2) {
            return head.substring(0, 2);

        }
        return "en";
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private void touchProgressIfPossible(User user, Exercise ex) {
        if (user != null
                && ex != null
                && ex.getLesson() != null
                && ex.getLesson().getCourse() != null
                && user.getId() != null
                && ex.getLesson().getCourse().getId() != null) {
            courseProgress.touchLastActivity(user.getId(), ex.getLesson().getCourse().getId());
            enrollmentRepo.findByUser_IdAndCourse_Id(user.getId(), ex.getLesson().getCourse().getId()).ifPresent(en -> {
                en.setLastActivityAt(Instant.now());
                en.setCurrentLesson(ex.getLesson());
                enrollmentRepo.save(en);
            });
        }
    }

    private String defaultQuizFeedback(boolean correct, String lang) {
        String n = normalizeLang(lang);
        return switch (n) {
            case "pl" -> correct ? "Poprawna odpowiedź." : "Niepoprawna odpowiedź.";
            case "de" -> correct ? "Richtige Antwort." : "Falsche Antwort.";
            case "es" -> correct ? "Respuesta correcta." : "Respuesta incorrecta.";
            case "en" -> correct ? "Correct answer." : "Incorrect answer.";
            default -> correct ? "Correct answer." : "Incorrect answer.";
        };
    }
}
