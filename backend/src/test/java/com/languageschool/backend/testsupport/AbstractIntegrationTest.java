package com.languageschool.backend.testsupport;

import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserLevel;
import com.languageschool.backend.entity.UserRole;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.repository.UserLevelRepository;
import com.languageschool.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void registerDbProps(DynamicPropertyRegistry registry) {
        var pg = TestPostgresContainer.get();
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLevelRepository userLevelRepository;

    @Autowired
    private ProficiencyLevelRepository proficiencyLevelRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedBaseDataForTests() {
        seedUserLevels();
        seedProficiencyLevels();
        seedLanguages();
        seedAdmin();
    }

    private void seedUserLevels() {
        if (userLevelRepository.count() == 0) {
            userLevelRepository.save(new UserLevel(1, 0));
            userLevelRepository.save(new UserLevel(2, 200));
            userLevelRepository.save(new UserLevel(3, 500));
            userLevelRepository.save(new UserLevel(4, 900));
            userLevelRepository.save(new UserLevel(5, 1400));
        }
    }

    private void seedProficiencyLevels() {
        if (proficiencyLevelRepository.count() > 0) {
            return;
        }
        List<ProficiencyLevel> levels = List.of(
                ProficiencyLevel.builder().code("A1").name("Beginner").orderNumber(1).build(),
                ProficiencyLevel.builder().code("A2").name("Elementary").orderNumber(2).build(),
                ProficiencyLevel.builder().code("B1").name("Intermediate").orderNumber(3).build(),
                ProficiencyLevel.builder().code("B2").name("Upper-intermediate").orderNumber(4).build(),
                ProficiencyLevel.builder().code("C1").name("Advanced").orderNumber(5).build(),
                ProficiencyLevel.builder().code("C2").name("Proficient").orderNumber(6).build()
        );
        proficiencyLevelRepository.saveAll(levels);
    }

    private void seedLanguages() {
        if (languageRepository.count() > 0) {
            return;
        }
        List<Language> langs = List.of(
                Language.builder().code("en").name("English").build(),
                Language.builder().code("pl").name("Polish").build(),
                Language.builder().code("de").name("German").build(),
                Language.builder().code("fr").name("French").build(),
                Language.builder().code("es").name("Spanish").build(),
                Language.builder().code("it").name("Italian").build()
        );
        languageRepository.saveAll(langs);
    }

    private void seedAdmin() {
        if (userRepository.findByLogin("admin").isPresent() || userRepository.findByEmail("admin@example.com").isPresent()) {
            return;
        }
        User admin = new User();
        admin.setLogin("admin");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setName("Ada");
        admin.setSurname("Admin");
        admin.setRole(UserRole.admin);
        userRepository.save(admin);
    }
}
