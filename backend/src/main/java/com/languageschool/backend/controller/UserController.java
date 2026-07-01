package com.languageschool.backend.controller;

import com.languageschool.backend.dto.common.PageResponse;
import com.languageschool.backend.dto.user.ChangeEmailRequest;
import com.languageschool.backend.dto.user.ChangeLoginRequest;
import com.languageschool.backend.dto.user.ChangePasswordRequest;
import com.languageschool.backend.dto.user.CreateUserRequest;
import com.languageschool.backend.dto.user.UpdateUserRequest;
import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.dto.user.admin.AdminChangeEmailRequest;
import com.languageschool.backend.dto.user.admin.AdminChangeLoginRequest;
import com.languageschool.backend.dto.user.admin.AdminSetPasswordRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    private static final Set<String> SORT_WHITELIST =
            Set.of("id", "login", "email", "createdAt", "lastLoginAt", "totalXp");

    public UserController(UserService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        UserDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public PageResponse<UserDto> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String role,
                                      @ParameterObject
                                      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
                                      Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        Sort filteredSort = Sort.by(
                pageable.getSort().stream()
                        .filter(o -> SORT_WHITELIST.contains(o.getProperty()))
                        .collect(Collectors.toList())
        );
        if (filteredSort.isUnsorted()) {
            filteredSort = Sort.by(Sort.Order.desc("createdAt"));
        }
        Pageable p = PageRequest.of(pageable.getPageNumber(), size, filteredSort);
        return service.adminSearch(q, role, p);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{id}")
    public UserDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me")
    public UserDto getMe(Authentication auth) {
        return service.findByLogin(auth.getName()).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/users/me")
    public UserDto updateMe(@Valid @RequestBody UpdateUserRequest req, Authentication auth) {
        UserDto current = service.findByLogin(auth.getName()).orElseThrow(ApiException::notFound);
        return service.update(current.getId(), req);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> deleteMe(Authentication auth) {
        UserDto current = service.findByLogin(auth.getName()).orElseThrow(ApiException::notFound);
        service.delete(current.getId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/users/me/login")
    public ResponseEntity<Void> changeLogin(@Valid @RequestBody ChangeLoginRequest req, Authentication auth) {
        service.changeLogin(auth.getName(), req.getNewLogin(), req.getCurrentPassword());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/users/me/email")
    public ResponseEntity<Void> changeEmail(@Valid @RequestBody ChangeEmailRequest req, Authentication auth) {
        service.changeEmail(auth.getName(), req.getNewEmail(), req.getCurrentPassword());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
        service.changePassword(auth.getName(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/login-admin")
    public UserDto changeLoginAdmin(@PathVariable Long id, @Valid @RequestBody AdminChangeLoginRequest req) {
        return service.changeLoginAdmin(id, req.getNewLogin());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/email-admin")
    public UserDto changeEmailAdmin(@PathVariable Long id, @Valid @RequestBody AdminChangeEmailRequest req) {
        return service.changeEmailAdmin(id, req.getNewEmail());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/password-admin")
    public ResponseEntity<Void> setPasswordAdmin(@PathVariable Long id, @Valid @RequestBody AdminSetPasswordRequest req) {
        service.setPasswordAdmin(id, req.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
