package com.languageschool.backend.service;

import com.languageschool.backend.dto.userSrs.UserSrsDto;

import java.time.Instant;
import java.util.List;

public interface UserSrsService {

    List<UserSrsDto> findByUser(Long userId);

    List<UserSrsDto> findDueByUser(Long userId, Instant dueBefore);

    UserSrsDto review(Long userId, Long exerciseId, Integer quality);
}
