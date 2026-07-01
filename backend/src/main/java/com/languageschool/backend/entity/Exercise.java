package com.languageschool.backend.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lesson", "correctOption"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "exercises")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE exercises SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ExerciseType type;

    @Lob
    @Column(name = "question", nullable = false)
    private String question;

    @Type(JsonType.class)
    @Column(name = "answer_schema", columnDefinition = "jsonb")
    private Map<String, Object> answerSchema;

    @Lob
    @Column(name = "sample_answer")
    private String sampleAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column(name = "xp", nullable = false)
    @Builder.Default
    private Integer xp = 0;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correct_option_id")
    private ExerciseOption correctOption;

    @Column(name = "passing_score", precision = 5, scale = 2)
    private BigDecimal passingScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
