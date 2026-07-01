package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.CourseEnrollment;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CourseRecommendationIntegrationTest extends AbstractIntegrationTest {

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
    CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    void generate_recommendations_does_not_return_enrolled_courses() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        var user = userRepository.findByLogin(login).orElseThrow();

        Course c1 = createCourse("C1");
        Course c2 = createCourse("C2");
        Course c3 = createCourse("C3");

        courseEnrollmentRepository.save(CourseEnrollment.builder()
                .user(user)
                .course(c2)
                .build());

        String reqJson = """
                {
                  "limit": 10,
                  "learningLanguageCode": "en",
                  "fromLanguageCode": "pl",
                  "levelCode": "A1"
                }
                """;

        mockMvc.perform(
                        post("/api/users/me/recommendations/generate")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reqJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].courseId").isNotEmpty())
                .andExpect(jsonPath("$[?(@.courseId == " + c2.getId() + ")]").doesNotExist());

        mockMvc.perform(
                        get("/api/users/me/recommendations/top")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .param("limit", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private Course createCourse(String title) {
        Language pl = languageRepository.findById("pl").orElseThrow();
        Language en = languageRepository.findById("en").orElseThrow();
        ProficiencyLevel a1 = proficiencyLevelRepository.findById("A1").orElseThrow();

        Course c = Course.builder()
                .title(title + "_" + UUID.randomUUID())
                .description("d")
                .learningLanguage(en)
                .fromLanguage(pl)
                .proficiencyLevel(a1)
                .build();
        return courseRepository.save(c);
    }
}
