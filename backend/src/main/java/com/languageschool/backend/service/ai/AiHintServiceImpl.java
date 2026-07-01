package com.languageschool.backend.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.config.AiRuntimeLimits;
import com.languageschool.backend.dto.ai.HintDtos.HintRequest;
import com.languageschool.backend.dto.ai.HintDtos.HintResponse;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LlmLog;
import com.languageschool.backend.entity.LlmStatus;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.LlmLogRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.AiHintService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.languageschool.backend.service.ai.LlmSupport.formatUsageSuffix;
import static com.languageschool.backend.service.ai.LlmSupport.mapStatus;
import static com.languageschool.backend.service.ai.LlmSupport.redact;
import static com.languageschool.backend.service.ai.LlmSupport.truncate;

@Service
@ConditionalOnBean(ClaudeClient.class)
public class AiHintServiceImpl implements AiHintService {

    private final ClaudeClient claude;
    private final ExerciseRepository exRepo;
    private final LlmLogRepository llmLogRepo;
    private final UserRepository userRepo;
    private final CourseEnrollmentRepository enrollmentRepo;
    private final AiRuntimeLimits limits;
    private final AiProps props;
    private final ObjectMapper om = new ObjectMapper();

    public AiHintServiceImpl(ClaudeClient claude,
                             ExerciseRepository exRepo,
                             LlmLogRepository llmLogRepo,
                             UserRepository userRepo,
                             CourseEnrollmentRepository enrollmentRepo,
                             AiRuntimeLimits limits,
                             AiProps props) {
        this.claude = claude;
        this.exRepo = exRepo;
        this.llmLogRepo = llmLogRepo;
        this.userRepo = userRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.limits = limits;
        this.props = props;
    }

    @Override
    @Transactional
    public HintResponse hint(HintRequest request, String login) {
        Exercise ex = exRepo.findById(request.getExerciseId())
                .orElseThrow(ApiException::notFound);

        String userAnswerRaw = request.getUserAnswer();
        String userAnswer = userAnswerRaw == null ? "" : userAnswerRaw.trim();
        int maxUserChars = limits.maxUserMessageChars() <= 0 ? Integer.MAX_VALUE : limits.maxUserMessageChars();
        if (userAnswer.length() > maxUserChars) {
            userAnswer = userAnswer.substring(0, maxUserChars);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }

        String effectiveLogin;
        if (login != null && !login.isBlank()) {
            SecurityUtils.ensureOwnerOrAdmin(auth, login);
            effectiveLogin = login;
        } else {
            effectiveLogin = auth.getName();
        }

        boolean isAdmin = SecurityUtils.isAdmin(auth);

        User requester = userRepo.findByLogin(effectiveLogin)
                .orElseThrow(ApiException::notFound);

        if (!isAdmin && ex.getLesson() != null && ex.getLesson().getCourse() != null) {
            Long courseId = ex.getLesson().getCourse().getId();
            if (courseId != null) {
                boolean enrolled = enrollmentRepo.existsByUser_IdAndCourse_Id(requester.getId(), courseId);
                if (!enrolled) {
                    throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
                }
            }
        }

        String fromLanguageCode = resolveFromLanguageCode(ex);
        Integer reqMaxHints = request.getMaxHints();
        int cappedHints = reqMaxHints == null ? 3 : Math.max(1, Math.min(5, reqMaxHints));

        String sys = """
                You are a language-learning assistant and tutor for a text-only online language-learning app. You are helping with WRITTEN exercises inside the app, not real-life situations.
                There is no audio, picture, video, classroom, teacher or live scene. Use only the text of the exercise and the student's answer. Do not imagine or refer to any external scene or image.
                If the request is outside language learning, output {"correct":false,"feedback":"","hints":[]}.
                Refuse sexual, explicit, hateful, violent, illegal or personal-data requests and output the same empty structure.
                Reply ONLY strict JSON: {"correct":true|false,"feedback":"string","hints":["string","...", "..."]}.
                Write feedback and hints in: %s.
                feedback: 1–2 short sentences, similar in style to feedback after an incorrect answer in a test. Briefly say what is missing, unclear or wrong and what the student should focus on next, in a friendly, encouraging tone.
                hints: exactly %d short, concrete tips (max 120 characters each) that guide the student step by step towards a better answer, without revealing the exact full answer. Always return exactly %d items in the "hints" array, even if you split your guidance into smaller steps.
                Where helpful, you may include one or two key words or very short phrases in the target language (the language the student should answer in), but never give the full correct answer, full sentence or complete solution.
                Never talk about clicking buttons, choosing options, the user interface, or instructions like "select an option" or "answer the question".
                Never tell the student to look at, observe or listen to a picture, photo, recording, classroom, board, teacher or scene. Focus only on the language, text and content of the exercise.
                For quiz-exerciseType questions, do NOT fabricate the list of options and do NOT reveal which option is correct.
                If the student has not given a real answer yet (for example the answer is "HINT_ONLY" or extremely short), treat it as "no answer yet":
                - feedback: gently suggest what aspect of the question they should think about (grammar, tense, vocabulary, meaning).
                - hints: give content-focused clues that help understand what kind of answer is expected.
                """.formatted(fromLanguageCode, cappedHints, cappedHints);

        StringBuilder body = new StringBuilder();
        body.append("Exercise exerciseType: ").append(ex.getType().name().toLowerCase(Locale.ROOT)).append("\n");
        body.append("Context: text-only exercise inside an online language-learning app; there is no picture, audio, classroom or live scene. Do not imagine any real teacher or situation.\n");
        body.append("Question: ").append(nz(ex.getQuestion())).append("\n");

        if (ex.getType() == ExerciseType.fill_in && ex.getAnswerSchema() != null) {
            try {
                Object acc = ((Map<?, ?>) ex.getAnswerSchema()).get("acceptable");
                @SuppressWarnings("unchecked")
                List<String> acceptable = acc instanceof List<?> l ? (List<String>) (List<?>) l : List.of();
                body.append("Acceptable answers (for checking only; do not reveal): ").append(acceptable).append("\n");
            } catch (Exception ignored) {
                body.append("Acceptable answers (for checking only; do not reveal): []\n");
            }
        }

        if (ex.getType() == ExerciseType.quiz) {
            body.append("This is a multiple-choice style question. Do not list or guess concrete options.\n");
        }

        body.append("Student answer: ").append(nz(userAnswer));

        List<Map<String, Object>> messages = List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "text", "text", body.toString()))
        ));

        long t0 = System.nanoTime();
        final ClaudeClient.CompletionResult result;
        try {
            result = claude.completeWithUsage(sys, messages, limits.maxOutputTokens());
        } catch (RestClientException exx) {
            long latencyMs = (System.nanoTime() - t0) / 1_000_000;
            String safePrompt = """
                    HINT REQUEST
                    Question: %s
                    Student answer: %s
                    """.formatted(ex.getQuestion(), nz(userAnswer));
            LlmLog log = new LlmLog();
            setUserIfPresent(log, effectiveLogin);
            log.setLesson(ex.getLesson());
            log.setInteractionType(InteractionType.hint);
            log.setPrompt(truncate(redact(safePrompt), 4000));
            log.setResponse(truncate(redact(String.valueOf(exx.getMessage())), 2000));
            log.setModel(props.model());
            log.setTokensIn(0);
            log.setTokensOut(0);
            log.setLatencyMs((int) latencyMs);
            Map<String, Object> params = Map.of(
                    "maxOutputTokens", limits.maxOutputTokens(),
                    "fromLanguageCode", fromLanguageCode,
                    "exerciseType", ex.getType().name()
            );
            log.setParams(params);
            log.setStatus(mapStatus(exx));
            llmLogRepo.save(log);
            throw ApiException.serviceUnavailable();
        }
        long latencyMs = (System.nanoTime() - t0) / 1_000_000;

        String raw = result.text();
        Map<String, Object> parsed = safeMapRelaxed(raw);

        boolean correct = parsed.get("correct") instanceof Boolean b
                ? b
                : "true".equalsIgnoreCase(String.valueOf(parsed.get("correct")));
        String feedback = toStr(parsed.get("feedback"));
        if (feedback.isBlank()) {
            feedback = defaultFeedback(fromLanguageCode);
        }

        @SuppressWarnings("unchecked")
        List<String> hintsParsed = parsed.get("hints") instanceof List<?> l
                ? (List<String>) (List<?>) l
                : List.of();
        List<String> hints = normalizeHints(hintsParsed, cappedHints);

        String safePrompt = """
                HINT REQUEST
                Question: %s
                Student answer: %s
                """.formatted(ex.getQuestion(), nz(userAnswer));

        LlmLog log = new LlmLog();
        setUserIfPresent(log, effectiveLogin);
        log.setLesson(ex.getLesson());
        log.setInteractionType(InteractionType.hint);
        log.setPrompt(truncate(redact(safePrompt), 4000));
        log.setResponse(truncate(redact(raw), 4000) + formatUsageSuffix(result));
        log.setModel(props.model());
        log.setTokensIn(result.promptTokens() == null ? 0 : result.promptTokens());
        log.setTokensOut(result.completionTokens() == null ? 0 : result.completionTokens());
        log.setLatencyMs((int) latencyMs);
        Map<String, Object> params = Map.of(
                "maxOutputTokens", limits.maxOutputTokens(),
                "fromLanguageCode", fromLanguageCode,
                "exerciseType", ex.getType().name()
        );
        log.setParams(params);
        log.setStatus(LlmStatus.ok);
        llmLogRepo.save(log);

        updateProgressIfPossible(requester, ex);

        return HintResponse.builder()
                .correct(correct)
                .feedback(feedback)
                .hints(hints)
                .build();
    }

    private List<String> normalizeHints(List<String> in, int cap) {
        if (in == null) {
            in = List.of();
        }
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > 120) {
                t = t.substring(0, 120);
            }
            out.add(t);
            if (out.size() >= cap) {
                break;
            }
        }
        if (out.isEmpty()) {
            return List.of();
        }
        while (out.size() < cap) {
            out.add(out.get(out.size() - 1));
        }
        return out;
    }

    private void setUserIfPresent(LlmLog log, String login) {
        if (login != null) {
            userRepo.findByLogin(login).ifPresent(log::setUser);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            userRepo.findByLogin(auth.getName()).ifPresent(log::setUser);
        }
    }

    private String resolveFromLanguageCode(Exercise ex) {
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

    private Map<String, Object> safeMapRelaxed(String json) {
        Map<String, Object> v = tryParseMap(json);
        if (!v.isEmpty()) {
            return v;
        }
        String s = json == null ? "" : json.trim();
        s = stripCodeFence(s);
        v = tryParseMap(s);
        if (!v.isEmpty()) {
            return v;
        }
        String sliced = sliceFirstJsonObject(s);
        v = tryParseMap(sliced);
        if (!v.isEmpty()) {
            return v;
        }
        String repaired = fixTrailingCommas(sliced);
        v = tryParseMap(repaired);
        if (!v.isEmpty()) {
            return v;
        }
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
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl >= 0) {
                t = t.substring(firstNl + 1);
            }
        }
        if (t.endsWith("```")) {
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) {
                t = t.substring(0, lastFence);
            }
        }
        return t.trim();
    }

    private String sliceFirstJsonObject(String s) {
        if (s == null) {
            return "{}";
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return "{}";
    }

    private String fixTrailingCommas(String s) {
        if (s == null) {
            return "{}";
        }
        return s.replaceAll(",\\s*([}\\]])", "$1");
    }

    private static String nz(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String toStr(Object v) {
        String s = v == null ? "" : v.toString();
        return s == null ? "" : s.trim();
    }

    private String defaultFeedback(String lang) {
        return switch (normalizeLang(lang)) {
            case "pl" ->
                    "Twoja odpowiedź nie jest jeszcze w pełni poprawna. Skup się na kluczowych słowach i strukturze zdania.";
            case "de" ->
                    "Deine Antwort ist noch nicht ganz korrekt. Konzentriere dich auf die wichtigsten Wörter und die Satzstruktur.";
            case "es" ->
                    "Tu respuesta todavía no es del todo correcta. Fíjate en las palabras clave y en la estructura de la frase.";
            case "en" ->
                    "Your answer is not fully correct yet. Focus on the key words and the structure of the sentence.";
            default ->
                    "Your answer is not fully correct yet. Focus on the key words and the structure of the sentence.";
        };
    }

    private void updateProgressIfPossible(User user, Exercise ex) {
        if (user != null
                && ex != null
                && ex.getLesson() != null
                && ex.getLesson().getCourse() != null
                && user.getId() != null
                && ex.getLesson().getCourse().getId() != null) {
            enrollmentRepo.findByUser_IdAndCourse_Id(user.getId(), ex.getLesson().getCourse().getId()).ifPresent(en -> {
                en.setLastActivityAt(Instant.now());
                en.setCurrentLesson(ex.getLesson());
                enrollmentRepo.save(en);
            });
        }
    }
}
