package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.common.PageResponse;
import com.languageschool.backend.dto.user.CreateUserRequest;
import com.languageschool.backend.dto.user.UpdateUserRequest;
import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserRole;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.util.SecurityUtils;
import com.languageschool.backend.service.UserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.languageschool.backend.util.SecurityUtils.requireAdmin;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDto create(CreateUserRequest req) {
        String login = trim(req.getLogin());
        String email = lower(req.getEmail());
        String password = nullSafe(req.getPassword());

        if (login == null || login.isEmpty() || email == null || email.isEmpty() || password.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }

        if (repo.findByLogin(login).isPresent()) {
            throw ApiException.conflict(ErrorCode.LOGIN_TAKEN);
        }
        if (repo.findByEmail(email).isPresent()) {
            throw ApiException.conflict(ErrorCode.EMAIL_TAKEN);
        }

        User u = new User();
        u.setLogin(login);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setName(trim(req.getName()));
        u.setSurname(trim(req.getSurname()));
        u.setRole(UserRole.user);
        return toDto(repo.save(u));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {
        return repo.findById(id).map(u -> {
            ensureOwnerOrAdmin(u.getLogin());
            return toDto(u);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findByLogin(String login) {
        return repo.findByLogin(login).map(u -> {
            ensureOwnerOrAdmin(u.getLogin());
            return toDto(u);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        requireAdmin(SecurityContextHolder.getContext().getAuthentication());
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public UserDto update(Long id, UpdateUserRequest req) {
        User u = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(u.getLogin());

        if (req.getName() != null) {
            u.setName(trim(req.getName()));
        }
        if (req.getSurname() != null) {
            u.setSurname(trim(req.getSurname()));
        }

        return toDto(repo.save(u));
    }

    @Override
    public void delete(Long id) {
        User u = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(u.getLogin());
        repo.delete(u);
    }

    @Override
    public void changeLogin(String currentLogin, String newLogin, String currentPassword) {
        User u = repo.findByLogin(currentLogin)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(u.getLogin());

        if (!passwordEncoder.matches(nullSafe(currentPassword), u.getPasswordHash())) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        String normalized = trim(newLogin);
        if (normalized == null || normalized.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }

        Optional<User> byLogin = repo.findByLogin(normalized);
        if (byLogin.isPresent() && !byLogin.get().getId().equals(u.getId())) {
            throw ApiException.conflict(ErrorCode.LOGIN_TAKEN);
        }

        u.setLogin(normalized);
        repo.save(u);
    }

    @Override
    public void changeEmail(String currentLogin, String newEmail, String currentPassword) {
        User u = repo.findByLogin(currentLogin)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(u.getLogin());

        if (!passwordEncoder.matches(nullSafe(currentPassword), u.getPasswordHash())) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        String normalized = lower(newEmail);
        if (normalized == null || normalized.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }

        Optional<User> byEmail = repo.findByEmail(normalized);
        if (byEmail.isPresent() && !byEmail.get().getId().equals(u.getId())) {
            throw ApiException.conflict(ErrorCode.EMAIL_TAKEN);
        }

        u.setEmail(normalized);
        repo.save(u);
    }

    @Override
    public void changePassword(String currentLogin, String currentPassword, String newPassword) {
        User u = repo.findByLogin(currentLogin)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(u.getLogin());

        if (!passwordEncoder.matches(nullSafe(currentPassword), u.getPasswordHash())) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        String newPass = nullSafe(newPassword);
        if (newPass.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }

        u.setPasswordHash(passwordEncoder.encode(newPass));
        repo.save(u);
    }

    @Override
    public UserDto changeLoginAdmin(Long id, String newLogin) {
        requireAdmin(SecurityContextHolder.getContext().getAuthentication());

        User u = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        String normalized = trim(newLogin);
        if (normalized == null || normalized.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }
        Optional<User> byLogin = repo.findByLogin(normalized);
        if (byLogin.isPresent() && !byLogin.get().getId().equals(u.getId())) {
            throw ApiException.conflict(ErrorCode.LOGIN_TAKEN);
        }
        u.setLogin(normalized);
        return toDto(repo.save(u));
    }

    @Override
    public UserDto changeEmailAdmin(Long id, String newEmail) {
        requireAdmin(SecurityContextHolder.getContext().getAuthentication());

        User u = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        String normalized = lower(newEmail);
        if (normalized == null || normalized.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }
        Optional<User> byEmail = repo.findByEmail(normalized);
        if (byEmail.isPresent() && !byEmail.get().getId().equals(u.getId())) {
            throw ApiException.conflict(ErrorCode.EMAIL_TAKEN);
        }
        u.setEmail(normalized);
        return toDto(repo.save(u));
    }

    @Override
    public void setPasswordAdmin(Long id, String newPassword) {
        requireAdmin(SecurityContextHolder.getContext().getAuthentication());

        User u = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        String newPass = nullSafe(newPassword);
        if (newPass.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.BAD_REQUEST);
        }
        u.setPasswordHash(passwordEncoder.encode(newPass));
        repo.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> adminSearch(String q, String role, Pageable pageable) {
        requireAdmin(SecurityContextHolder.getContext().getAuthentication());

        Specification<User> spec = Specification.allOf(List.of(qLike(q), roleEq(role)));
        Page<User> page = repo.findAll(spec, pageable);
        List<UserDto> content = page.getContent().stream().map(this::toDto).toList();

        return PageResponse.<UserDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    public UserDto updateAvatarPath(Long id, String avatarPath) {
        User u = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(u.getLogin());
        u.setAvatarPath(avatarPath);
        return toDto(repo.save(u));
    }

    private Specification<User> qLike(String q) {
        return (root, query, cb) -> {
            if (q == null || q.trim().isEmpty()) {
                return null;
            }
            String p = "%" + q.trim().toLowerCase() + "%";
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(cb.lower(root.get("login")), p));
            ors.add(cb.like(cb.lower(root.get("email")), p));
            ors.add(cb.like(cb.lower(root.get("name")), p));
            ors.add(cb.like(cb.lower(root.get("surname")), p));
            ors.add(cb.like(cb.lower(root.get("role").as(String.class)), p));
            try {
                Long id = Long.valueOf(q.trim());
                ors.add(cb.equal(root.get("id"), id));
            } catch (NumberFormatException ignored) {
            }
            return cb.or(ors.toArray(Predicate[]::new));
        };
    }

    private Specification<User> roleEq(String role) {
        return (root, query, cb) -> {
            if (role == null || role.isEmpty()) {
                return null;
            }
            try {
                UserRole r = UserRole.valueOf(role);
                return cb.equal(root.get("role"), r);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        };
    }

    private UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .login(u.getLogin())
                .email(u.getEmail())
                .name(u.getName())
                .surname(u.getSurname())
                .role(u.getRole())
                .totalXp(u.getTotalXp())
                .avatarPath(u.getAvatarPath())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private String lower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }
}
