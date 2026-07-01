package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userLessonProgress.CreateUserLessonProgressRequest;
import com.languageschool.backend.dto.userLessonProgress.UpdateUserLessonProgressRequest;
import com.languageschool.backend.dto.userLessonProgress.UserLessonProgressDto;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.LessonStatus;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserLessonProgress;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.UserLessonProgressRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.UserLessonProgressService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.data.domain.Sort;
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
public class UserLessonProgressServiceImpl implements UserLessonProgressService {

    private final UserLessonProgressRepository repo;
    private final UserRepository userRepo;
    private final LessonRepository lessonRepo;
    private final CourseEnrollmentRepository enrollmentRepo;

    public UserLessonProgressServiceImpl(UserLessonProgressRepository repo,
                                         UserRepository userRepo,
                                         LessonRepository lessonRepo,
                                         CourseEnrollmentRepository enrollmentRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.lessonRepo = lessonRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    @Override
    public List<UserLessonProgressDto> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
        return repo.findAll(Sort.by(Sort.Direction.DESC, "lastActivityAt")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public UserLessonProgressDto create(CreateUserLessonProgressRequest req) {
        User user = meUser();
        Lesson lesson = lessonRepo.findById(req.getLessonId()).orElseThrow(ApiException::notFound);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        Long courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        if (!isAdmin && courseId != null) {
            boolean enrolled = enrollmentRepo.existsByUser_IdAndCourse_Id(user.getId(), courseId);
            if (!enrolled) {
                throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
            }
        }
        if (repo.findByUser_IdAndLesson_Id(user.getId(), lesson.getId()).isPresent()) {
            throw ApiException.conflict(ErrorCode.ALREADY_EXISTS);
        }
        UserLessonProgress p = new UserLessonProgress();
        p.setUser(user);
        p.setLesson(lesson);
        p.setStatus(LessonStatus.in_progress);
        p.setLastActivityAt(Instant.now());
        return toDto(repo.save(p));
    }

    @Override
    public Optional<UserLessonProgressDto> findById(Long id) {
        return repo.findById(id).map(p -> {
            ensureOwnerOrAdmin(p.getUser() != null ? p.getUser().getLogin() : null);
            return toDto(p);
        });
    }

    @Override
    public Optional<UserLessonProgressDto> findByUserAndLesson(Long userId, Long lessonId) {
        String owner = userRepo.findById(userId).orElseThrow(ApiException::notFound).getLogin();
        ensureOwnerOrAdmin(owner);
        return repo.findByUser_IdAndLesson_Id(userId, lessonId).map(this::toDto);
    }

    @Override
    public List<UserLessonProgressDto> findByUser(Long userId) {
        String owner = userRepo.findById(userId).orElseThrow(ApiException::notFound).getLogin();
        ensureOwnerOrAdmin(owner);
        return repo.findByUser_Id(userId).stream().map(this::toDto).toList();
    }

    @Override
    public List<UserLessonProgressDto> findByUser(Long userId, LessonStatus status) {
        String owner = userRepo.findById(userId).orElseThrow(ApiException::notFound).getLogin();
        ensureOwnerOrAdmin(owner);
        return repo.findByUser_IdAndStatus(userId, status).stream().map(this::toDto).toList();
    }

    @Override
    public List<UserLessonProgressDto> findByLesson(Long lessonId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
        return repo.findByLesson_Id(lessonId).stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public UserLessonProgressDto update(Long id, UpdateUserLessonProgressRequest req) {
        UserLessonProgress p = repo.findById(id).orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(p.getUser() != null ? p.getUser().getLogin() : null);
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
            if (req.getStatus() == LessonStatus.completed && p.getCompletedAt() == null) {
                p.setCompletedAt(Instant.now());
            }
            if (req.getStatus() != LessonStatus.completed) {
                p.setCompletedAt(null);
            }
        }
        p.setLastActivityAt(Instant.now());
        return toDto(repo.save(p));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        UserLessonProgress p = repo.findById(id).orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(p.getUser() != null ? p.getUser().getLogin() : null);
        repo.delete(p);
    }

    private User meUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        return userRepo.findByLogin(auth.getName()).orElseThrow(ApiException::notFound);
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }

    private UserLessonProgressDto toDto(UserLessonProgress p) {
        return UserLessonProgressDto.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .lessonId(p.getLesson().getId())
                .status(p.getStatus())
                .completedAt(p.getCompletedAt())
                .lastActivityAt(p.getLastActivityAt())
                .build();
    }
}
