package com.languageschool.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "exercise", "chosenOption"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "exercise_attempts")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE exercise_attempts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Lob
    @Column(name = "submitted_answer")
    private String submittedAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_option_id")
    private ExerciseOption chosenOption;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private boolean correct = false;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Lob
    @Column(name = "feedback")
    private String feedback;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
