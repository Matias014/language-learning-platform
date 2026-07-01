package com.languageschool.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class FileStorageProps {
    @Value("${files.storage.root:photos}")
    private String root;

    public Path rootDir() {
        return Path.of(root).toAbsolutePath().normalize();
    }

    public Path avatarsDir() {
        return rootDir().resolve("avatars");
    }

    public Path countryIconsDir() {
        return rootDir().resolve("country");
    }

    public Path achievementIconsDir() {
        return rootDir().resolve("achievements");
    }
}
