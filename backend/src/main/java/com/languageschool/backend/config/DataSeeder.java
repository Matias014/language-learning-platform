package com.languageschool.backend.config;

import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserLevel;
import com.languageschool.backend.entity.UserRole;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.repository.UserLevelRepository;
import com.languageschool.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(100)
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserLevelRepository levelRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final LanguageRepository languageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      UserLevelRepository levelRepository,
                      ProficiencyLevelRepository proficiencyLevelRepository,
                      LanguageRepository languageRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.levelRepository = levelRepository;
        this.proficiencyLevelRepository = proficiencyLevelRepository;
        this.languageRepository = languageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUserLevels();
        seedProficiencyLevels();
        seedLanguages();
        seedAdmin();
    }

    private void seedUserLevels() {
        if (levelRepository.count() == 0) {
            levelRepository.save(new UserLevel(1, 0));
            levelRepository.save(new UserLevel(2, 200));
            levelRepository.save(new UserLevel(3, 500));
            levelRepository.save(new UserLevel(4, 900));
            levelRepository.save(new UserLevel(5, 1400));
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
