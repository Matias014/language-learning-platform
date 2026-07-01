package com.languageschool.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "languages")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE languages SET deleted_at = CURRENT_TIMESTAMP WHERE code = ? AND deleted_at IS NULL")
public class Language {

    @Id
    @Column(name = "code", length = 10)
    @EqualsAndHashCode.Include
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
