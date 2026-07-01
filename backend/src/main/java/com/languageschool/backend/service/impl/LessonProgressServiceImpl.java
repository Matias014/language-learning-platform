package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.lessonProgress.LessonProgressDto;
import com.languageschool.backend.entity.Lesson;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.CourseEnrollmentRepository;
import com.languageschool.backend.repository.ExerciseAwardRepository;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.LessonRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.LessonProgressService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonProgressServiceImpl implements LessonProgressService {

    private final ExerciseRepository exerciseRepo;
    private final ExerciseAwardRepository awardRepo;
    private final LessonRepository lessonRepo;
    private final UserRepository userRepo;
    private final CourseEnrollmentRepository enrollmentRepo;

    public LessonProgressServiceImpl(ExerciseRepository exerciseRepo,
                                     ExerciseAwardRepository awardRepo,
                                     LessonRepository lessonRepo,
                                     UserRepository userRepo,
                                     CourseEnrollmentRepository enrollmentRepo) {
        this.exerciseRepo = exerciseRepo;
        this.awardRepo = awardRepo;
        this.lessonRepo = lessonRepo;
        this.userRepo = userRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    @Override
    public LessonProgressDto getMyForLesson(Long lessonId, String login) {
        ensureOwnerOrAdmin(login);
        User user = userRepo.findByLogin(login).orElseThrow(ApiException::notFound);
        Lesson lesson = lessonRepo.findById(lessonId).orElseThrow(ApiException::notFound);
        Long courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        if (courseId != null && !enrollmentRepo.existsByUser_IdAndCourse_Id(user.getId(), courseId)) {
            throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
        }
        int percent = computePercent(user.getId(), lesson.getId());
        return LessonProgressDto.builder()
                .lessonId(lesson.getId())
                .progressPercent(percent)
                .build();
    }

    @Override
    public List<LessonProgressDto> listMyByCourse(Long courseId, String login) {
        ensureOwnerOrAdmin(login);
        User user = userRepo.findByLogin(login).orElseThrow(ApiException::notFound);
        if (!enrollmentRepo.existsByUser_IdAndCourse_Id(user.getId(), courseId)) {
            throw ApiException.forbidden(ErrorCode.NOT_ENROLLED);
        }
        List<Lesson> lessons = lessonRepo.findByCourse_IdOrderByOrderNumberAsc(courseId);
        return lessons.stream()
                .map(l -> LessonProgressDto.builder()
                        .lessonId(l.getId())
                        .progressPercent(computePercent(user.getId(), l.getId()))
                        .build())
                .toList();
    }

    private int computePercent(Long userId, Long lessonId) {
        long total = exerciseRepo.countByLesson_Id(lessonId);
        if (total <= 0) {
            return 0;
        }
        long completed = awardRepo.countDistinctExercisesByUserAndLesson(userId, lessonId);
        long r = Math.round(100.0 * completed / total);
        if (r < 0) return 0;
        if (r > 100) return 100;
        return (int) r;
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }
}
