package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.courseProgress.CourseProgressDto;
import com.languageschool.backend.entity.CourseEnrollment;
import com.languageschool.backend.entity.CourseStatus;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.ExerciseAwardRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.CourseProgressService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseProgressServiceImpl implements CourseProgressService {

    private final CourseEnrollmentRepository enrollmentRepo;
    private final ExerciseRepository exerciseRepo;
    private final ExerciseAwardRepository awardRepo;
    private final UserRepository userRepo;

    public CourseProgressServiceImpl(CourseEnrollmentRepository enrollmentRepo,
                                     ExerciseRepository exerciseRepo,
                                     ExerciseAwardRepository awardRepo,
                                     UserRepository userRepo) {
        this.enrollmentRepo = enrollmentRepo;
        this.exerciseRepo = exerciseRepo;
        this.awardRepo = awardRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    @Override
    public void recalcEnrollment(Long enrollmentId) {
        CourseEnrollment ce = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(ApiException::notFound);
        recalcAndSave(ce, true);
    }

    @Transactional
    @Override
    public void recalcForUserCourse(Long userId, Long courseId) {
        CourseEnrollment ce = enrollmentRepo.findByUser_IdAndCourse_Id(userId, courseId).orElse(null);
        if (ce == null) {
            return;
        }
        recalcAndSave(ce, true);
    }

    @Transactional
    @Override
    public void recalcAllForCourse(Long courseId) {
        List<CourseEnrollment> list = enrollmentRepo.findByCourse_Id(courseId);
        for (CourseEnrollment ce : list) {
            recalcAndSave(ce, false);
        }
    }

    @Transactional
    @Override
    public void touchLastActivity(Long userId, Long courseId) {
        enrollmentRepo.findByUser_IdAndCourse_Id(userId, courseId).ifPresent(ce -> {
            ce.setLastActivityAt(Instant.now());
            enrollmentRepo.save(ce);
        });
    }

    @Override
    public List<CourseProgressDto> listMy(String login) {
        ensureOwnerOrAdmin(login);
        User me = userRepo.findByLogin(login).orElseThrow(ApiException::notFound);
        List<CourseEnrollment> enrollments = enrollmentRepo.findByUser_Id(me.getId());
        List<CourseProgressDto> out = new ArrayList<>(enrollments.size());
        for (CourseEnrollment ce : enrollments) {
            Long courseId = ce.getCourse() != null ? ce.getCourse().getId() : null;
            if (courseId == null) {
                continue;
            }
            int percent = computePercent(me.getId(), courseId);
            out.add(CourseProgressDto.builder()
                    .courseId(courseId)
                    .progressPercent(percent)
                    .build());
        }
        return out;
    }

    @Override
    public CourseProgressDto getMyForCourse(Long courseId, String login) {
        ensureOwnerOrAdmin(login);
        User me = userRepo.findByLogin(login).orElseThrow(ApiException::notFound);
        boolean enrolled = enrollmentRepo.existsByUser_IdAndCourse_Id(me.getId(), courseId);
        if (!enrolled) {
            throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
        }
        int percent = computePercent(me.getId(), courseId);
        return CourseProgressDto.builder()
                .courseId(courseId)
                .progressPercent(percent)
                .build();
    }

    @Transactional
    protected void recalcAndSave(CourseEnrollment ce, boolean bumpActivity) {
        Long courseId = ce.getCourse().getId();
        Long userId = ce.getUser().getId();
        long total = exerciseRepo.countByLesson_Course_Id(courseId);
        long completed = total == 0 ? 0 : awardRepo.countDistinctExercisesByUserAndCourse(userId, courseId);
        boolean finished = total > 0 && completed == total;
        if (finished) {
            if (ce.getStatus() != CourseStatus.completed) {
                ce.setStatus(CourseStatus.completed);
            }
            if (ce.getCompletedAt() == null) {
                ce.setCompletedAt(Instant.now());
            }
        } else {
            if (ce.getStatus() == CourseStatus.completed) {
                ce.setStatus(CourseStatus.in_progress);
                ce.setCompletedAt(null);
            }
        }
        if (bumpActivity) {
            ce.setLastActivityAt(Instant.now());
        }
        enrollmentRepo.save(ce);
    }

    private int computePercent(Long userId, Long courseId) {
        long total = exerciseRepo.countByLesson_Course_Id(courseId);
        if (total <= 0) {
            return 0;
        }
        long completed = awardRepo.countDistinctExercisesByUserAndCourse(userId, courseId);
        double p = 100.0 * completed / total;
        long r = Math.round(p);
        if (r < 0) return 0;
        if (r > 100) return 100;
        return (int) r;
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }
}
