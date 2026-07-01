package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.courseRecommendation.CourseRecommendationDto;
import com.languageschool.backend.entity.Course;
import com.languageschool.backend.entity.CourseEnrollment;
import com.languageschool.backend.entity.CourseRecommendation;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.CourseRecommendationRepository;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.CourseRecommendationService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    private final CourseRecommendationRepository repo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final CourseEnrollmentRepository enrollmentRepo;

    @PersistenceContext
    private EntityManager em;

    public CourseRecommendationServiceImpl(CourseRecommendationRepository repo,
                                           UserRepository userRepo,
                                           CourseRepository courseRepo,
                                           CourseEnrollmentRepository enrollmentRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    @Transactional
    @Override
    public List<CourseRecommendationDto> generateForUser(Long userId,
                                                         Integer limit,
                                                         String learningLanguageCode,
                                                         String fromLanguageCode,
                                                         String levelCode) {
        ensureOwnerOrAdmin(userId);
        int lim = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        User u = userRepo.findById(userId).orElseThrow(ApiException::notFound);

        Set<Long> enrolled = new HashSet<>();
        for (CourseEnrollment ce : enrollmentRepo.findByUser_Id(userId)) {
            if (ce.getCourse() != null) {
                enrolled.add(ce.getCourse().getId());
            }
        }

        List<Course> filtered = new ArrayList<>();
        for (Course c : courseRepo.findAll()) {
            if (enrolled.contains(c.getId())) continue;
            if (learningLanguageCode != null && !learningLanguageCode.isBlank()) {
                if (c.getLearningLanguage() == null || !learningLanguageCode.equals(c.getLearningLanguage().getCode()))
                    continue;
            }
            if (fromLanguageCode != null && !fromLanguageCode.isBlank()) {
                if (c.getFromLanguage() == null || !fromLanguageCode.equals(c.getFromLanguage().getCode())) continue;
            }
            if (levelCode != null && !levelCode.isBlank()) {
                if (c.getProficiencyLevel() == null || !levelCode.equals(c.getProficiencyLevel().getCode())) continue;
            }
            filtered.add(c);
        }

        if (filtered.isEmpty()) {
            repo.deleteByUser_Id(userId);
            return List.of();
        }

        List<Long> courseIds = filtered.stream().map(Course::getId).toList();
        Map<Long, Integer> enrollCounts = new HashMap<>();
        for (CourseEnrollment ce : enrollmentRepo.findByCourse_IdIn(courseIds)) {
            if (ce.getCourse() != null) {
                Long cid = ce.getCourse().getId();
                enrollCounts.merge(cid, 1, Integer::sum);
            }
        }

        filtered.sort((a, b) -> {
            int pa = enrollCounts.getOrDefault(a.getId(), 0);
            int pb = enrollCounts.getOrDefault(b.getId(), 0);
            int cmp = Integer.compare(pb, pa);
            if (cmp != 0) return cmp;
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        repo.deleteByUser_Id(userId);

        List<CourseRecommendationDto> out = new ArrayList<>();
        int taken = 0;
        for (Course c : filtered) {
            if (taken >= lim) break;

            CourseRecommendation cr = new CourseRecommendation();
            cr.setUser(u);
            cr.setCourse(c);

            BigDecimal score = BigDecimal.valueOf(99.9999d - taken).setScale(4, RoundingMode.HALF_UP);
            if (score.signum() < 0) {
                score = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            cr.setScore(score);

            out.add(toDto(repo.save(cr)));
            taken++;
        }
        return out;
    }

    @Override
    public List<CourseRecommendationDto> findTopForUser(Long userId, Integer limit) {
        ensureOwnerOrAdmin(userId);
        int lim = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        return em.createQuery(
                        "select cr from CourseRecommendation cr where cr.user.id=:uid order by cr.score desc",
                        CourseRecommendation.class)
                .setParameter("uid", userId)
                .setMaxResults(lim)
                .getResultList()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void ensureOwnerOrAdmin(Long userId) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String login = userRepo.findById(userId).orElseThrow(ApiException::notFound).getLogin();
        SecurityUtils.ensureOwnerOrAdmin(a, login);
    }

    private CourseRecommendationDto toDto(CourseRecommendation e) {
        Long userId = e.getUser() != null ? e.getUser().getId() : null;
        Long courseId = e.getCourse() != null ? e.getCourse().getId() : null;
        return CourseRecommendationDto.builder()
                .id(e.getId())
                .userId(userId)
                .courseId(courseId)
                .score(e.getScore())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
