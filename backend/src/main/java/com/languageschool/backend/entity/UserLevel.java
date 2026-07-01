package com.languageschool.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "user_levels")
public class UserLevel {

    @Id
    @Column(name = "level", nullable = false)
    @EqualsAndHashCode.Include
    private Integer level;

    @Column(name = "required_xp", nullable = false)
    private Integer requiredXp;
}
