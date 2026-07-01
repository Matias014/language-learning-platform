package com.languageschool.backend.entity;

import com.languageschool.backend.security.crypto.StringAttributeEncryptorConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "lesson", "exerciseAttempt", "chatSession"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "llm_logs")
public class LlmLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_attempt_id")
    private ExerciseAttempt exerciseAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    @Builder.Default
    private InteractionType interactionType = InteractionType.chat;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Type(JsonType.class)
    @Column(name = "params", columnDefinition = "jsonb")
    private Map<String, Object> params;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private LlmStatus status = LlmStatus.ok;

    @Lob
    @Convert(converter = StringAttributeEncryptorConverter.class)
    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Lob
    @Convert(converter = StringAttributeEncryptorConverter.class)
    @Column(name = "response", nullable = false)
    private String response;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
