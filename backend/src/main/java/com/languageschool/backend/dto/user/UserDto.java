package com.languageschool.backend.dto.user;

import com.languageschool.backend.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class UserDto {
    Long id;
    String login;
    String email;
    String name;
    String surname;
    UserRole role;
    Integer totalXp;
    String avatarPath;
    Instant createdAt;
    Instant updatedAt;
    Instant lastLoginAt;
}
