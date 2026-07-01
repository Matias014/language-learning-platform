package com.languageschool.backend.service;

import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.dto.common.PageResponse;
import com.languageschool.backend.dto.user.CreateUserRequest;
import com.languageschool.backend.dto.user.UpdateUserRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserDto create(CreateUserRequest req);

    Optional<UserDto> findById(Long id);

    Optional<UserDto> findByLogin(String login);

    List<UserDto> findAll();

    UserDto update(Long id, UpdateUserRequest req);

    void delete(Long id);

    void changeLogin(String currentLogin, String newLogin, String currentPassword);

    void changeEmail(String currentLogin, String newEmail, String currentPassword);

    void changePassword(String currentLogin, String currentPassword, String newPassword);

    UserDto changeLoginAdmin(Long id, String newLogin);

    UserDto changeEmailAdmin(Long id, String newEmail);

    void setPasswordAdmin(Long id, String newPassword);

    PageResponse<UserDto> adminSearch(String q, String role, Pageable pageable);

    UserDto updateAvatarPath(Long id, String avatarPath);
}
