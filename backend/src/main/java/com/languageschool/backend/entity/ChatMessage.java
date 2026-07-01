package com.languageschool.backend.entity;

import com.languageschool.backend.security.crypto.StringAttributeEncryptorConverter;
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
@ToString(exclude = "session")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "chat_messages")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE chat_messages SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 20)
    private MessageSender sender;

    @Lob
    @Convert(converter = StringAttributeEncryptorConverter.class)
    @Column(name = "message", nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
