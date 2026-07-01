package com.languageschool.backend.dto.userSrs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class UserSrsDto {
    Long id;
    Long userId;
    Long exerciseId;
    Instant dueAt;
    Integer intervalDays;
    Integer repetitions;
    Integer lastQuality;
    Instant createdAt;
    Instant updatedAt;
}
