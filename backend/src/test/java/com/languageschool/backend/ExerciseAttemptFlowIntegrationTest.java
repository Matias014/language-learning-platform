package com.languageschool.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.dto.ai.GradeResponse;
import com.languageschool.backend.dto.ai.HintDtos;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.ExerciseAttemptRepository;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.service.AiGradingService;
import com.languageschool.backend.service.AiHintService;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExerciseAttemptFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LanguageRepository languageRepository;
    @Autowired
    ProficiencyLevelRepository proficiencyLevelRepository;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    LessonRepository lessonRepository;
    @Autowired
    ExerciseRepository exerciseRepository;
    @Autowired
    ExerciseOptionRepository exerciseOptionRepository;
    @Autowired
    ExerciseAttemptRepository exerciseAttemptRepository;

    @MockitoBean
    AiGradingService aiGradingService;

    @MockitoBean
    AiHintService aiHintService;

    @Test
    void create_attempt_then_evaluate_ok_then_hint_ok() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        Exercise exercise = createQuizExercise();

        long attemptId = createAttempt(session.accessToken(), exercise.getId(), exercise.getCorrectOption().getId());

        when(aiGradingService.gradeAttempt(eq(attemptId), anyString()))
                .thenReturn(new GradeResponse(true, "OK", List.of("h1"), attemptId, 10));

        mockMvc.perform(
                        post("/api/exercise-attempts/" + attemptId + "/evaluations")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.feedback").value("OK"))
                .andExpect(jsonPath("$.attemptId").value((int) attemptId))
                .andExpect(jsonPath("$.awardedXp").value(10))
                .andExpect(jsonPath("$.hints").isArray());

        when(aiHintService.hint(any(HintDtos.HintRequest.class), anyString()))
                .thenReturn(new HintDtos.HintResponse(true, "Hint OK", List.of("Try this")));

        String hintJson = """
                {
                  "exerciseId": %d,
                  "userAnswer": "answer",
                  "maxHints": 2
                }
                """.formatted(exercise.getId());

        mockMvc.perform(
                        post("/api/ai/hints")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(hintJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.feedback").value("Hint OK"))
                .andExpect(jsonPath("$.hints").isArray());
    }

    @Test
    void evaluate_service_unavailable_returns_503_and_code() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        Exercise exercise = createQuizExercise();
        long attemptId = createAttempt(session.accessToken(), exercise.getId(), exercise.getCorrectOption().getId());

        when(aiGradingService.gradeAttempt(eq(attemptId), anyString()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE));

        mockMvc.perform(
                        post("/api/exercise-attempts/" + attemptId + "/evaluations")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void create_attempt_validation_error_returns_400() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        mockMvc.perform(
                        post("/api/exercise-attempts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private Exercise createQuizExercise() {
        Language pl = languageRepository.findById("pl").orElseThrow();
        Language en = languageRepository.findById("en").orElseThrow();
        ProficiencyLevel a1 = proficiencyLevelRepository.findById("A1").orElseThrow();

        Course course = Course.builder()
                .title("t_" + UUID.randomUUID())
                .description("d")
                .learningLanguage(en)
                .fromLanguage(pl)
                .proficiencyLevel(a1)
                .build();
        course = courseRepository.save(course);

        Lesson lesson = Lesson.builder()
                .course(course)
                .title("l_" + UUID.randomUUID())
                .description("d")
                .orderNumber(1)
                .build();
        lesson = lessonRepository.save(lesson);

        Exercise ex = Exercise.builder()
                .lesson(lesson)
                .orderNumber(1)
                .type(ExerciseType.quiz)
                .difficulty(DifficultyLevel.easy)
                .question("Pick one")
                .sampleAnswer("A")
                .xp(10)
                .build();
        ex = exerciseRepository.save(ex);

        ExerciseOption o1 = ExerciseOption.builder().exercise(ex).orderNumber(1).content("A").build();
        ExerciseOption o2 = ExerciseOption.builder().exercise(ex).orderNumber(2).content("B").build();

        o1 = exerciseOptionRepository.save(o1);
        exerciseOptionRepository.save(o2);

        ex.setCorrectOption(o1);
        return exerciseRepository.save(ex);
    }

    private long createAttempt(String accessToken, long exerciseId, Long chosenOptionId) throws Exception {
        String json = """
                {
                  "exerciseId": %d,
                  "submittedAnswer": "answer",
                  "chosenOptionId": %d,
                  "durationSeconds": 12
                }
                """.formatted(exerciseId, chosenOptionId);

        var res = mockMvc.perform(
                        post("/api/exercise-attempts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsByteArray());
        long attemptId = body.path("id").asLong();

        assertThat(exerciseAttemptRepository.findById(attemptId)).isPresent();
        return attemptId;
    }
}
