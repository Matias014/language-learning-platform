package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseOption;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.ExerciseOptionRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AiDisabledIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    void hint_without_ai_returns_503() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        Exercise ex = createQuizExercise();

        String json = """
                {
                  "exerciseId": %d,
                  "userAnswer": "answer",
                  "maxHints": 2
                }
                """.formatted(ex.getId());

        mockMvc.perform(
                        post("/api/ai/hints")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_DISABLED"));
    }

    @Test
    void evaluate_without_ai_grading_returns_503() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        mockMvc.perform(
                        post("/api/exercise-attempts/999999/evaluations")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_GRADING_DISABLED"));
    }

    @Test
    void admin_exercise_gen_without_ai_returns_503() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);
        var admin = auth.login("admin", "password");

        Lesson lesson = createLesson();

        String json = """
                {
                  "exerciseType": "quiz",
                  "difficultyLevel": "easy",
                  "topic": "travel",
                  "count": 2,
                  "xp": 10
                }
                """;

        mockMvc.perform(
                        post("/api/lessons/" + lesson.getId() + "/exercises")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_DISABLED"));
    }

    private Lesson createLesson() {
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
        return lessonRepository.save(lesson);
    }

    private Exercise createQuizExercise() {
        Lesson lesson = createLesson();

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
}
