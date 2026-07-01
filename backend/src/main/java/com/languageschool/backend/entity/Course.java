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
@ToString(exclude = {"learningLanguage", "fromLanguage", "proficiencyLevel"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "courses")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE courses SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_language_code", referencedColumnName = "code", nullable = false)
    private Language learningLanguage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_language_code", referencedColumnName = "code", nullable = false)
    private Language fromLanguage;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level_code", referencedColumnName = "code", nullable = false)
    private ProficiencyLevel proficiencyLevel;

    @Column(name = "country_icon_path", length = 2048)
    private String countryIconPath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
