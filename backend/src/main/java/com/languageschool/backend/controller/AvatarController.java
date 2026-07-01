package com.languageschool.backend.controller;

import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.FileStorageService;
import com.languageschool.backend.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AvatarController {

    private final FileStorageService storage;
    private final UserService userService;

    public AvatarController(FileStorageService storage, UserService userService) {
        this.storage = storage;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/users/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        String publicPath = storage.storeUserAvatar(id, file);
        return userService.updateAvatarPath(id, publicPath);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping(path = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto uploadMe(Authentication auth, @RequestPart("file") MultipartFile file) {
        Long me = userService.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        String publicPath = storage.storeUserAvatar(me, file);
        return userService.updateAvatarPath(me, publicPath);
    }
}
