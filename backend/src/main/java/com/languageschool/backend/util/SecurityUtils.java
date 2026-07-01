package com.languageschool.backend.util;

import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public static Long requireMeId(UserService userService, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        UserDto me = userService.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound);
        return me.getId();
    }

    public static void ensureOwnerOrAdmin(UserService userService,
                                          Authentication auth,
                                          Long ownerUserId) {
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        if (isAdmin(auth)) {
            return;
        }
        Long meId = requireMeId(userService, auth);
        if (ownerUserId == null || !meId.equals(ownerUserId)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
    }

    public static void ensureOwnerOrAdmin(Authentication auth, String ownerLogin) {
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        if (isAdmin(auth)) {
            return;
        }
        if (ownerLogin == null || !ownerLogin.equalsIgnoreCase(auth.getName())) {
            throw new AccessDeniedException("FORBIDDEN");
        }
    }

    public static void requireAdmin(Authentication auth) {
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("FORBIDDEN");
        }
    }
}
