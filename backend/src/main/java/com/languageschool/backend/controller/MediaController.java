package com.languageschool.backend.controller;

import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MediaController {

    private final FileStorageService storage;

    public MediaController(FileStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/media/avatars/{name}")
    public ResponseEntity<Resource> avatar(@PathVariable String name) {
        Resource resource = storage.loadAvatar(name);
        return buildResponse(resource);
    }

    @GetMapping("/media/country/{name}")
    public ResponseEntity<Resource> country(@PathVariable String name) {
        Resource resource = storage.loadCountryIcon(name);
        return buildResponse(resource);
    }

    @GetMapping("/media/achievements/{name}")
    public ResponseEntity<Resource> achievement(@PathVariable String name) {
        Resource resource = storage.loadAchievementIcon(name);
        return buildResponse(resource);
    }

    private ResponseEntity<Resource> buildResponse(Resource resource) {
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw ApiException.notFound();
        }
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
