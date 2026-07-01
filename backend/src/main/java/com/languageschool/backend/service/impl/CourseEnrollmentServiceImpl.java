package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.courseEnrollment.CourseEnrollmentDto;
import com.languageschool.backend.dto.courseEnrollment.CreateCourseEnrollmentRequest;
import com.languageschool.backend.dto.courseEnrollment.UpdateCourseEnrollmentRequest;
import com.languageschool.backend.entity.CourseEnrollment;
import com.languageschool.backend.entity.CourseStatus;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.CourseRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.CourseEnrollmentService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CourseEnrollmentServiceImpl implements CourseEnrollmentService {

    private final CourseEnrollmentRepository repo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final LessonRepository lessonRepo;

    @PersistenceContext
    private EntityManager em;

    public CourseEnrollmentServiceImpl(CourseEnrollmentRepository repo,
                                       UserRepository userRepo,
                                       CourseRepository courseRepo,
                                       LessonRepository lessonRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.lessonRepo = lessonRepo;
    }

    @Override
    public List<CourseEnrollmentDto> findAll() {
        ensureAdmin();
        return repo.findAll(Sort.by(Sort.Direction.DESC, "lastActivityAt")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public CourseEnrollmentDto create(CreateCourseEnrollmentRequest req) {
        User user = meUser();
        var course = courseRepo.findById(req.getCourseId())
                .orElseThrow(ApiException::notFound);

        if (repo.findByUser_IdAndCourse_Id(user.getId(), course.getId()).isPresent()) {
            throw ApiException.conflict(ErrorCode.ALREADY_ENROLLED);
        }

        Lesson firstLesson = em.createQuery(
                        "select l from Lesson l where l.course.id = :cid order by l.orderNumber asc", Lesson.class)
                .setParameter("cid", course.getId())
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        CourseEnrollment ce = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .build();
        ce.setStatus(CourseStatus.in_progress);
        ce.setStartedAt(Instant.now());
        ce.setCompletedAt(null);
        ce.setLastActivityAt(Instant.now());
        ce.setCurrentLesson(firstLesson);
        return toDto(repo.save(ce));
    }

    @Override
    public CourseEnrollmentDto getById(Long id) {
        CourseEnrollment ce = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(ce.getUser() != null ? ce.getUser().getLogin() : null);
        return toDto(ce);
    }

    @Override
    public Optional<CourseEnrollmentDto> findByUserAndCourse(Long userId, Long courseId) {
        String owner = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(owner);
        return repo.findByUser_IdAndCourse_Id(userId, courseId).map(this::toDto);
    }

    @Override
    public List<CourseEnrollmentDto> findByUser(Long userId) {
        String owner = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(owner);
        return repo.findByUser_Id(userId).stream().map(this::toDto).toList();
    }

    @Override
    public List<CourseEnrollmentDto> findByCourse(Long courseId) {
        ensureAdmin();
        return repo.findByCourse_Id(courseId).stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public CourseEnrollmentDto update(Long id, UpdateCourseEnrollmentRequest req) {
        CourseEnrollment ce = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(ce.getUser() != null ? ce.getUser().getLogin() : null);

        if (req.getCurrentLessonId() != null) {
            Lesson lesson = lessonRepo.findById(req.getCurrentLessonId())
                    .orElseThrow(ApiException::notFound);
            if (lesson.getCourse() == null ||
                    !lesson.getCourse().getId().equals(ce.getCourse().getId())) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.LESSON_NOT_IN_COURSE);
            }
            ce.setCurrentLesson(lesson);
        }

        ce.setLastActivityAt(Instant.now());
        return toDto(repo.save(ce));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        CourseEnrollment ce = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(ce.getUser() != null ? ce.getUser().getLogin() : null);
        repo.delete(ce);
    }

    private User meUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        return userRepo.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound);
    }

    private void ensureAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }

    private CourseEnrollmentDto toDto(CourseEnrollment ce) {
        return CourseEnrollmentDto.builder()
                .id(ce.getId())
                .userId(ce.getUser() != null ? ce.getUser().getId() : null)
                .courseId(ce.getCourse() != null ? ce.getCourse().getId() : null)
                .currentLessonId(ce.getCurrentLesson() != null ? ce.getCurrentLesson().getId() : null)
                .status(ce.getStatus())
                .startedAt(ce.getStartedAt())
                .completedAt(ce.getCompletedAt())
                .lastActivityAt(ce.getLastActivityAt())
                .build();
    }
}
