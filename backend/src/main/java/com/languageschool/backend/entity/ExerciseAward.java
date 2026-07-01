package com.languageschool.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"attempt"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "exercise_awards")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE exercise_awards SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class ExerciseAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExerciseAttempt attempt;

    @Column(name = "awarded_xp", nullable = false)
    private Integer awardedXp;

    @CreationTimestamp
    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
