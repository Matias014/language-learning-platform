package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.entity.*;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.service.AiExerciseService;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AiExerciseAdminIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired LanguageRepository languageRepository;
    @Autowired ProficiencyLevelRepository proficiencyLevelRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired LessonRepository lessonRepository;

    @MockitoBean
    AiExerciseService aiExerciseService;

    @Test
    void admin_can_generate_exercises() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);
        var admin = auth.login("admin", "password");

        Lesson lesson = createLesson();

        when(aiExerciseService.generate(
                eq(lesson.getId()),
                any(ExerciseType.class),
                any(DifficultyLevel.class),
                anyString(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of(101L, 102L));

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
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(101))
                .andExpect(jsonPath("$[1]").value(102));
    }

    @Test
    void user_cannot_generate_exercises_403() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var user = auth.register(login, email, "password");

        Lesson lesson = createLesson();

        String json = """
                {
                  "exerciseType": "quiz",
                  "difficultyLevel": "easy",
                  "topic": "travel",
                  "count": 1,
                  "xp": 10
                }
                """;

        mockMvc.perform(
                        post("/api/lessons/" + lesson.getId() + "/exercises")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken())
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(status().isForbidden());
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
}
