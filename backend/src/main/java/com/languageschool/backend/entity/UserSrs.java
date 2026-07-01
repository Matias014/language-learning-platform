package com.languageschool.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "exercise"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "user_srs")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE user_srs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class UserSrs {

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

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    private Integer intervalDays = 0;

    @Column(name = "repetitions", nullable = false)
    @Builder.Default
    private Integer repetitions = 0;

    @Column(name = "last_quality")
    private Integer lastQuality;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
