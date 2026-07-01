package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.repository.CourseRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SrsIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    void review_creates_srs_entry_and_due_endpoint_returns_it() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        Exercise ex = createWritingExercise();

        String reviewJson = """
                {
                  "exerciseId": %d,
                  "quality": 5
                }
                """.formatted(ex.getId());

        mockMvc.perform(
                        post("/api/users/me/srs/review")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reviewJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(ex.getId().intValue()))
                .andExpect(jsonPath("$.lastQuality").value(5))
                .andExpect(jsonPath("$.dueAt").exists());

        mockMvc.perform(
                        get("/api/users/me/srs")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Instant before = Instant.now().plus(2, ChronoUnit.DAYS);
        mockMvc.perform(
                        get("/api/users/me/srs/due")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .param("before", before.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void review_validation_quality_out_of_range_returns_400() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        Exercise ex = createWritingExercise();

        String badReviewJson = """
                {
                  "exerciseId": %d,
                  "quality": 6
                }
                """.formatted(ex.getId());

        mockMvc.perform(
                        post("/api/users/me/srs/review")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(badReviewJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void srs_summary_and_this_week_endpoints_work() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        mockMvc.perform(
                        get("/api/users/me/srs/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/users/me/srs/this-week")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk());
    }

    private Exercise createWritingExercise() {
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
                .type(ExerciseType.writing)
                .difficulty(DifficultyLevel.easy)
                .question("Write something")
                .sampleAnswer("A")
                .xp(5)
                .build();

        return exerciseRepository.save(ex);
    }
}
