package com.languageschool.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Table(name = "proficiency_levels")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE proficiency_levels SET deleted_at = CURRENT_TIMESTAMP WHERE code = ? AND deleted_at IS NULL")
public class ProficiencyLevel {

    @Id
    @Column(name = "code", length = 2)
    @EqualsAndHashCode.Include
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$")
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    @NotBlank
    private String name;

    @Min(1)
    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
