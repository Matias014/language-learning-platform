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
@ToString(exclude = "exercise")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "exercise_options")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE exercise_options SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class ExerciseOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
