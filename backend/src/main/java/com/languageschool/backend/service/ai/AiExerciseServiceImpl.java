package com.languageschool.backend.service.ai;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.languageschool.backend.config.AiRuntimeLimits;
import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.LlmLog;
import com.languageschool.backend.entity.LlmStatus;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.LlmLogRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.AiExerciseService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.text.Normalizer;
import java.util.*;

import static com.languageschool.backend.service.ai.LlmSupport.*;

@Service
@ConditionalOnBean(ClaudeClient.class)
public class AiExerciseServiceImpl implements AiExerciseService {

    private final ClaudeClient claude;
    private final LessonRepository lessonRepo;
    private final ExerciseRepository exRepo;
    private final ExerciseOptionRepository optRepo;
    private final AiRuntimeLimits limits;
    private final LlmLogRepository llmLogRepo;
    private final UserRepository userRepo;
    private final AiProps props;
    private final ObjectMapper om = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .build();

    @PersistenceContext
    private EntityManager em;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "at", "by", "with", "from", "as",
            "de", "la", "le", "et", "und", "der", "die", "das", "zu", "im", "den", "dem",
            "i", "w", "z", "na", "do", "lub", "albo", "o", "u", "za", "od", "po"
    );

    public AiExerciseServiceImpl(ClaudeClient claude,
                                 LessonRepository lessonRepo,
                                 ExerciseRepository exRepo,
                                 ExerciseOptionRepository optRepo,
                                 AiRuntimeLimits limits,
                                 LlmLogRepository llmLogRepo,
                                 UserRepository userRepo,
                                 AiProps props) {
        this.claude = claude;
        this.lessonRepo = lessonRepo;
        this.exRepo = exRepo;
        this.optRepo = optRepo;
        this.limits = limits;
        this.llmLogRepo = llmLogRepo;
        this.userRepo = userRepo;
        this.props = props;
    }

    @Override
    @Transactional
    public List<Long> generate(Long lessonId,
                               ExerciseType type,
                               DifficultyLevel difficulty,
                               String topic,
                               Integer count,
                               Integer xp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);

        Lesson lesson = lessonRepo.findById(lessonId).orElseThrow(ApiException::notFound);
        ExerciseType reqType = type;
        DifficultyLevel diff = difficulty;
        if (reqType == null) throw ApiException.badRequest(ErrorCode.TYPE_REQUIRED);
        if (diff == null) throw ApiException.badRequest(ErrorCode.DIFFICULTY_REQUIRED);

        LanguageContext lang = resolveLanguageContext(lesson);
        String learningLanguageCode = lang.learningCode();
        String learningLanguageName = lang.learningName();
        String fromLanguageCode = lang.fromCode();
        String fromLanguageName = lang.fromName();

        int need = Math.max(1, Optional.ofNullable(count).orElse(1));
        int defaultXp = xp != null ? Math.max(0, xp) : 10;

        String courseTitle = lesson.getCourse() != null ? nz(lesson.getCourse().getTitle()) : "";
        String levelCode = lesson.getCourse() != null && lesson.getCourse().getProficiencyLevel() != null
                ? nz(lesson.getCourse().getProficiencyLevel().getCode()) : "A1";
        String lessonTitle = nz(lesson.getTitle());
        String lessonDesc = nz(lesson.getDescription());
        String normalizedTopic = nz(topic).trim();

        Set<String> usedQ = new HashSet<>();
        List<Map<String, Object>> accepted = new ArrayList<>();
        int attempts = 0;

        while (accepted.size() < need && attempts < 8) {
            int remaining = need - accepted.size();

            String system = """
                    You are a content generator restricted to foreign-language learning only.
                    If the request is outside language learning topics, output [].
                    Refuse and output [] for sexual, explicit, hateful, violent, illegal or personal-data requests.
                    The learner is learning %s (%s).
                    Their support language is %s (%s), but you must not use testsupport in any of the returned fields.
                    All natural-language fields you return must be written entirely in the learning language %s.
                    Return STRICT JSON: an array of exactly %d objects with fields:
                    {"exerciseType":"quiz|fill_in|writing","question":"string","options":["..."],"correctIndex":0,"acceptable":["..."],"sampleAnswer":"string","topicTag":"string","correct":"string","answer":"string"}
                    Output only the JSON array. No code fences. No extra text.
                    """.formatted(
                    learningLanguageName, learningLanguageCode,
                    fromLanguageName, fromLanguageCode,
                    learningLanguageName,
                    remaining
            );

            String user = """
                    Course: %s (level %s)
                    Lesson: %s
                    Lesson description: %s
                    Learning language: %s (%s)
                    Support language (used only for the topic text): %s (%s)
                    Topic (verbatim, may be written in the support language; set as topicTag without translating): %s
                    Generate %d exercise(s) of exerciseType %s suitable for CEFR %s and difficulty_level %s.
                    All questions, options, acceptable answers and sampleAnswer must be in the learning language %s.
                    Avoid duplicates: %s
                    """.formatted(
                    courseTitle, levelCode,
                    lessonTitle,
                    lessonDesc,
                    learningLanguageName, learningLanguageCode,
                    fromLanguageName, fromLanguageCode,
                    normalizedTopic,
                    remaining,
                    reqType.name().toLowerCase(Locale.ROOT),
                    levelCode,
                    diff.name().toLowerCase(Locale.ROOT),
                    learningLanguageName,
                    usedQ.isEmpty() ? "[]" : usedQ.toString()
            );

            List<Map<String, Object>> msgs = List.of(Map.of(
                    "role", "user",
                    "content", List.of(Map.of("type", "text", "text", user))
            ));

            long t0 = System.nanoTime();
            final ClaudeClient.CompletionResult result;
            try {
                result = claude.completeWithUsage(system, msgs, limits.maxOutputTokens());
            } catch (RestClientException ex) {
                long latencyMs = (System.nanoTime() - t0) / 1_000_000;
                LlmLog logEntry = new LlmLog();
                if (auth != null && auth.getName() != null) {
                    userRepo.findByLogin(auth.getName()).ifPresent(logEntry::setUser);
                }
                logEntry.setLesson(lesson);
                logEntry.setInteractionType(InteractionType.generation);
                String safePrompt = """
                        EXERCISE GENERATION
                        Lesson: %s
                        Topic: %s
                        Type: %s
                        Difficulty: %s
                        """.formatted(lesson.getTitle(), nz(topic),
                        reqType.name().toLowerCase(Locale.ROOT),
                        diff.name().toLowerCase(Locale.ROOT));
                logEntry.setPrompt(truncate(redact(safePrompt), 4000));
                logEntry.setResponse(truncate(redact(String.valueOf(ex.getMessage())), 2000));
                logEntry.setModel(props.model());
                logEntry.setTokensIn(0);
                logEntry.setTokensOut(0);
                logEntry.setLatencyMs((int) latencyMs);
                Map<String, Object> p = new HashMap<>();
                p.put("maxOutputTokens", limits.maxOutputTokens());
                p.put("learningLanguageCode", learningLanguageCode);
                p.put("fromLanguageCode", fromLanguageCode);
                p.put("requestedCount", remaining);
                p.put("type", reqType.name());
                p.put("difficulty", diff.name());
                p.put("topic", normalizedTopic);
                logEntry.setParams(p);
                logEntry.setStatus(mapStatus(ex));
                llmLogRepo.save(logEntry);
                throw ApiException.serviceUnavailable();
            }
            long latencyMs = (System.nanoTime() - t0) / 1_000_000;

            String raw = result.text();

            LlmLog logEntry = new LlmLog();
            if (auth != null && auth.getName() != null) {
                userRepo.findByLogin(auth.getName()).ifPresent(logEntry::setUser);
            }
            logEntry.setLesson(lesson);
            logEntry.setInteractionType(InteractionType.generation);
            String safePrompt = """
                    EXERCISE GENERATION
                    Lesson: %s
                    Topic: %s
                    Type: %s
                    Difficulty: %s
                    """.formatted(lesson.getTitle(), nz(topic),
                    reqType.name().toLowerCase(Locale.ROOT),
                    diff.name().toLowerCase(Locale.ROOT));
            logEntry.setPrompt(truncate(redact(safePrompt), 4000));
            logEntry.setResponse(truncate(redact(raw), 4000) + formatUsageSuffix(result));
            logEntry.setModel(props.model());
            logEntry.setTokensIn(result.promptTokens() == null ? 0 : result.promptTokens());
            logEntry.setTokensOut(result.completionTokens() == null ? 0 : result.completionTokens());
            logEntry.setLatencyMs((int) latencyMs);
            Map<String, Object> p = new HashMap<>();
            p.put("maxOutputTokens", limits.maxOutputTokens());
            p.put("learningLanguageCode", learningLanguageCode);
            p.put("fromLanguageCode", fromLanguageCode);
            p.put("requestedCount", remaining);
            p.put("type", reqType.name());
            p.put("difficulty", diff.name());
            p.put("topic", normalizedTopic);
            logEntry.setParams(p);
            logEntry.setStatus(LlmStatus.ok);
            llmLogRepo.save(logEntry);

            List<Map<String, Object>> batch = safeArrayRelaxed(raw);
            for (Map<String, Object> it : batch) {
                if (accepted.size() >= need) break;
                ExerciseType exType = resolveType(it, reqType);
                if (exType != reqType) continue;
                String q = nz(it.get("question")).trim();
                if (q.isEmpty()) continue;
                String topicTag = nz(it.get("topicTag")).trim();
                boolean topicMatches = topicMatches(normalizedTopic, topicTag, attempts);
                if (!topicMatches) continue;
                if (usedQ.contains(q.toLowerCase(Locale.ROOT))) continue;
                if (!basicHeuristicsOk(it, exType)) continue;
                if (exType == ExerciseType.fill_in) {
                    List<String> acceptable = Optional.ofNullable(toStringList(it.get("acceptable"))).orElse(List.of());
                    if (acceptable.isEmpty() && attempts >= 2) {
                        acceptable = deriveAcceptable(it);
                    }
                    if (acceptable.isEmpty()) continue;
                    it.put("acceptable", acceptable);
                } else if (exType == ExerciseType.writing) {
                    String sample = nz(it.get("sampleAnswer")).trim();
                    if (sample.isEmpty() && attempts < 3) continue;
                } else if (exType == ExerciseType.quiz) {
                    List<String> opts = Optional.ofNullable(toStringList(it.get("options"))).orElse(List.of());
                    Integer correctIndex = toInt(it.get("correctIndex"));
                    if (opts.size() < 2) continue;
                    if (correctIndex == null || correctIndex < 0 || correctIndex >= opts.size()) {
                        Integer fixed = fixCorrectIndex(opts, it.get("correct"), it.get("answer"));
                        if (fixed != null) {
                            it.put("correctIndex", fixed);
                        } else if (correctIndex != null && correctIndex >= 1 && correctIndex <= opts.size() && attempts >= 2) {
                            it.put("correctIndex", correctIndex - 1);
                        } else {
                            continue;
                        }
                    }
                }
                accepted.add(it);
                usedQ.add(q.toLowerCase(Locale.ROOT));
            }
            attempts++;
        }

        if (accepted.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.UNPROCESSABLE_ENTITY);
        }

        Integer maxOrder = em.createQuery(
                        "select max(e.orderNumber) from Exercise e where e.lesson.id = :lessonId", Integer.class)
                .setParameter("lessonId", lessonId)
                .getSingleResult();
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

        List<Long> ids = new ArrayList<>(accepted.size());
        for (Map<String, Object> it : accepted) {
            Exercise ex = new Exercise();
            ex.setLesson(lesson);
            ExerciseType exType = resolveType(it, reqType);
            ex.setType(exType);
            ex.setQuestion(nz(it.get("question")));
            ex.setDifficulty(diff);
            ex.setXp(defaultXp);
            ex.setOrderNumber(nextOrder++);
            ex.setPassingScore(null);

            if (exType == ExerciseType.fill_in) {
                List<String> acceptable = Optional.ofNullable(toStringList(it.get("acceptable"))).orElse(List.of());
                if (acceptable.isEmpty()) continue;
                ex.setAnswerSchema(Map.of("acceptable", acceptable));
                ex.setSampleAnswer(null);
            } else if (exType == ExerciseType.writing) {
                String sample = nz(it.get("sampleAnswer")).trim();
                ex.setSampleAnswer(sample.isEmpty() ? null : sample);
                ex.setAnswerSchema(null);
            } else if (exType == ExerciseType.quiz) {
                List<String> opts = Optional.ofNullable(toStringList(it.get("options"))).orElse(List.of());
                Integer correctIndex = toInt(it.get("correctIndex"));
                if (opts.size() < 2 || correctIndex == null || correctIndex < 0 || correctIndex >= opts.size())
                    continue;
                ex.setAnswerSchema(null);
                ex.setSampleAnswer(null);
            }

            Exercise saved = exRepo.save(ex);

            if (exType == ExerciseType.quiz) {
                List<String> opts = Optional.ofNullable(toStringList(it.get("options"))).orElse(List.of());
                Integer correctIndex = toInt(it.get("correctIndex"));
                List<ExerciseOption> persisted = new ArrayList<>();
                int ord = 1;
                for (String o : opts) {
                    ExerciseOption eo = new ExerciseOption();
                    eo.setExercise(saved);
                    eo.setContent(o == null ? "" : o);
                    eo.setOrderNumber(ord++);
                    persisted.add(optRepo.save(eo));
                }
                int idx = correctIndex == null ? -1 : correctIndex;
                if (idx >= 0 && idx < persisted.size()) {
                    saved.setCorrectOption(persisted.get(idx));
                    exRepo.save(saved);
                }
            }

            ids.add(saved.getId());
        }

        return ids;
    }

    private List<Map<String, Object>> safeArrayRelaxed(String json) {
        List<Map<String, Object>> v = tryParseArray(json);
        if (!v.isEmpty()) return v;
        String s = json == null ? "" : json.trim();
        s = stripCodeFence(s);
        v = tryParseArray(s);
        if (!v.isEmpty()) return v;
        String sliced = sliceFirstJsonArray(s);
        v = tryParseArray(sliced);
        if (!v.isEmpty()) return v;
        String repaired = fixTrailingCommas(sliced);
        v = tryParseArray(repaired);
        if (!v.isEmpty()) return v;
        return List.of();
    }

    private List<Map<String, Object>> tryParseArray(String json) {
        try {
            return om.readValue(json == null ? "[]" : json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return List.of();
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

    private String sliceFirstJsonArray(String s) {
        if (s == null) return "[]";
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return "[]";
    }

    private String fixTrailingCommas(String s) {
        if (s == null) return "[]";
        return s.replaceAll(",\\s*([}\\]])", "$1");
    }

    private boolean basicHeuristicsOk(Map<String, Object> it, ExerciseType expected) {
        if (resolveType(it, expected) != expected) return false;
        String q = nz(it.get("question")).trim();
        return !q.isEmpty();
    }

    private ExerciseType resolveType(Map<String, Object> it, ExerciseType fallback) {
        String raw = nz(it.get("exerciseType")).trim();
        if (raw.isEmpty()) raw = nz(it.get("type")).trim();
        String t = raw.toLowerCase(Locale.ROOT);
        if (t.equals("quiz")) return ExerciseType.quiz;
        if (t.equals("fill_in") || t.equals("fill-in") || t.equals("fillin")) return ExerciseType.fill_in;
        if (t.equals("writing")) return ExerciseType.writing;
        return fallback;
    }

    private List<String> toStringList(Object v) {
        try {
            if (v instanceof List<?> l) {
                List<String> out = new ArrayList<>(l.size());
                for (Object o : l) out.add(o == null ? "" : String.valueOf(o));
                return out;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object v) {
        try {
            if (v instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static String nz(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String norm(String s) {
        String x = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        String n = Normalizer.normalize(x, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "");
    }

    private Set<String> tokens(String s) {
        String n = norm(s);
        String[] parts = n.split("\\W+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.length() < 2) continue;
            if (STOPWORDS.contains(t)) continue;
            out.add(t);
        }
        return out;
    }

    private boolean topicMatches(String requested, String tagged, int attempt) {
        String nt = norm(requested);
        String tg = norm(tagged);
        if (nt.isEmpty() || tg.isEmpty()) return true;
        if (tg.equals(nt) || tg.contains(nt) || nt.contains(tg)) return true;
        Set<String> a = tokens(nt);
        Set<String> b = tokens(tg);
        if (a.isEmpty() || b.isEmpty()) return false;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        double jaccard = (double) inter.size() / (double) (a.size() + b.size() - inter.size());
        double threshold = attempt < 2 ? 0.3 : 0.15;
        return jaccard >= threshold;
    }

    private List<String> deriveAcceptable(Map<String, Object> it) {
        List<String> out = new ArrayList<>();
        String sample = nz(it.get("sampleAnswer")).trim();
        if (!sample.isEmpty()) out.addAll(splitAnswers(sample));
        String answer = nz(it.get("answer")).trim();
        if (!answer.isEmpty()) out.addAll(splitAnswers(answer));
        if (out.isEmpty()) return out;
        List<String> cleaned = new ArrayList<>();
        for (String s : out) {
            String t = s == null ? "" : s.trim();
            if (!t.isEmpty()) cleaned.add(t);
        }
        LinkedHashSet<String> uniq = new LinkedHashSet<>(cleaned);
        return new ArrayList<>(uniq);
    }

    private List<String> splitAnswers(String s) {
        List<String> r = new ArrayList<>();
        if (s == null) return r;
        String t = s.trim();
        if (t.isEmpty()) return r;
        String[] parts = t.split("\\s*(,|;|/|\\|)\\s|\\s+or\\s+|\\s+lub\\s+");
        for (String p : parts) {
            String v = p == null ? "" : p.trim();
            if (!v.isEmpty()) r.add(v);
        }
        if (r.isEmpty()) r.add(t);
        return r;
    }

    private Integer fixCorrectIndex(List<String> options, Object correctField, Object answerField) {
        String cand = nz(correctField).trim();
        if (cand.isEmpty()) cand = nz(answerField).trim();
        if (cand.isEmpty()) return null;
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i) == null ? "" : options.get(i).trim();
            if (equalsIgnoreCaseTrim(opt, cand)) return i;
        }
        return null;
    }

    private boolean equalsIgnoreCaseTrim(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.equalsIgnoreCase(y);
    }

    private LanguageContext resolveLanguageContext(Lesson lesson) {
        String learningCode = null;
        String learningName = null;
        String fromCode = null;
        String fromName = null;

        try {
            if (lesson.getCourse() != null && lesson.getCourse().getLearningLanguage() != null) {
                var lang = lesson.getCourse().getLearningLanguage();
                learningCode = normalizeLang(lang.getCode());
                learningName = nz(lang.getName());
            }
        } catch (Exception ignored) {
        }

        try {
            if (lesson.getCourse() != null && lesson.getCourse().getFromLanguage() != null) {
                var lang = lesson.getCourse().getFromLanguage();
                fromCode = normalizeLang(lang.getCode());
                fromName = nz(lang.getName());
            }
        } catch (Exception ignored) {
        }

        if (learningCode == null || learningCode.isBlank()) {
            learningCode = "en";
        }
        if (learningName == null || learningName.isBlank()) {
            learningName = languageNameFromCode(learningCode);
        }
        if (fromCode == null || fromCode.isBlank()) {
            fromCode = learningCode;
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = languageNameFromCode(fromCode);
        }

        return new LanguageContext(learningCode, learningName, fromCode, fromName);
    }

    private String languageNameFromCode(String code) {
        String c = normalizeLang(code);
        return switch (c) {
            case "en" -> "English";
            case "pl" -> "Polish";
            case "de" -> "German";
            case "fr" -> "French";
            case "es" -> "Spanish";
            case "testsupport" -> "Italian";
            case "pt" -> "Portuguese";
            case "ru" -> "Russian";
            case "uk" -> "Ukrainian";
            case "cs" -> "Czech";
            default -> c;
        };
    }

    private record LanguageContext(String learningCode, String learningName,
                                   String fromCode, String fromName) {
    }

    private String normalizeLang(String code) {
        if (code == null) return "en";
        String c = code.trim().toLowerCase(Locale.ROOT);
        if (c.isBlank()) return "en";
        int dash = c.indexOf('-');
        String head = dash > 0 ? c.substring(0, dash) : c;
        if (head.length() >= 2) return head.substring(0, 2);
        return "en";
    }
}
