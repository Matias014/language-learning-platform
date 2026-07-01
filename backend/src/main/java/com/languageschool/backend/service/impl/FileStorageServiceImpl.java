package com.languageschool.backend.service.impl;

import com.languageschool.backend.config.FileStorageProps;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private enum FileKind {AVATAR, COUNTRY, ACHIEVEMENT}

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final Set<String> ALLOWED_MIME = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    private final FileStorageProps props;

    public FileStorageServiceImpl(FileStorageProps props) {
        this.props = props;
        init();
    }

    private void init() {
        try {
            Files.createDirectories(props.avatarsDir());
            Files.createDirectories(props.countryIconsDir());
            Files.createDirectories(props.achievementIconsDir());
        } catch (IOException e) {
            throw new IllegalStateException("STORAGE_INIT_ERROR", e);
        }
    }

    @Override
    public String storeUserAvatar(Long userId, MultipartFile file) {
        String name = buildSafeName("u" + userId, file.getOriginalFilename(), FileKind.AVATAR);
        Path target = props.avatarsDir().resolve(name);
        write(file, target, FileKind.AVATAR);
        return "/api/media/avatars/" + name;
    }

    @Override
    public String storeCourseCountryIcon(Long courseId, MultipartFile file) {
        String name = buildSafeName("c" + courseId, file.getOriginalFilename(), FileKind.COUNTRY);
        Path target = props.countryIconsDir().resolve(name);
        write(file, target, FileKind.COUNTRY);
        return "/api/media/country/" + name;
    }

    @Override
    public String storeAchievementIcon(Long achievementId, MultipartFile file) {
        String name = buildSafeName("a" + achievementId, file.getOriginalFilename(), FileKind.ACHIEVEMENT);
        Path target = props.achievementIconsDir().resolve(name);
        write(file, target, FileKind.ACHIEVEMENT);
        return "/api/media/achievements/" + name;
    }

    @Override
    public Resource loadAvatar(String filename) {
        String base = Paths.get(filename == null ? "" : filename).getFileName().toString();
        return new FileSystemResource(props.avatarsDir().resolve(base).normalize());
    }

    @Override
    public Resource loadCountryIcon(String filename) {
        String base = Paths.get(filename == null ? "" : filename).getFileName().toString();
        return new FileSystemResource(props.countryIconsDir().resolve(base).normalize());
    }

    @Override
    public Resource loadAchievementIcon(String filename) {
        String base = Paths.get(filename == null ? "" : filename).getFileName().toString();
        return new FileSystemResource(props.achievementIconsDir().resolve(base).normalize());
    }

    private void write(MultipartFile file, Path target, FileKind kind) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, kind == FileKind.AVATAR ? ErrorCode.AVATAR_EMPTY : ErrorCode.BAD_REQUEST);
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, kind == FileKind.AVATAR ? ErrorCode.AVATAR_TOO_LARGE : ErrorCode.FILE_TOO_LARGE);
            }
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
            if (contentType.contains("svg") || contentType.contains("xml")) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, kind == FileKind.AVATAR ? ErrorCode.AVATAR_UNSUPPORTED_TYPE : ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
            if (!ALLOWED_MIME.contains(contentType)) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, kind == FileKind.AVATAR ? ErrorCode.AVATAR_UNSUPPORTED_TYPE : ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
            byte[] bytes = file.getBytes();
            if (looksLikeSvg(bytes)) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, kind == FileKind.AVATAR ? ErrorCode.AVATAR_UNSUPPORTED_TYPE : ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
            String detected = detectImageType(bytes);
            if (detected == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, kind == FileKind.AVATAR ? ErrorCode.AVATAR_INVALID_FORMAT : ErrorCode.UNPROCESSABLE_ENTITY);
            }
            String ext = extOf(target.getFileName().toString());
            if (!matchesExt(ext, detected)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, kind == FileKind.AVATAR ? ErrorCode.AVATAR_INVALID_FORMAT : ErrorCode.UNPROCESSABLE_ENTITY);
            }
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("FILE_WRITE_ERROR", e);
        }
    }

    private String buildSafeName(String prefix, String original, FileKind kind) {
        String ext = extOf(original);
        if (ext.isEmpty() || !ALLOWED_EXT.contains(ext)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, kind == FileKind.AVATAR ? ErrorCode.AVATAR_UNSUPPORTED_TYPE : ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + "-" + uuid + "." + ext;
    }

    private String extOf(String original) {
        String name = original == null ? "" : StringUtils.cleanPath(original).toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }

    private boolean matchesExt(String ext, String detected) {
        if (ext == null || detected == null) {
            return false;
        }
        if (ext.equals(detected)) {
            return true;
        }
        return (ext.equals("jpg") && detected.equals("jpeg")) || (ext.equals("jpeg") && detected.equals("jpg"));
    }

    private String detectImageType(byte[] data) {
        if (data == null || data.length < 12) {
            return null;
        }
        if (isPng(data)) {
            return "png";
        }
        if (isJpeg(data)) {
            return "jpg";
        }
        if (isWebp(data)) {
            return "webp";
        }
        if (isGif(data)) {
            return "gif";
        }
        return null;
    }

    private boolean isPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89
                && b[1] == 0x50
                && b[2] == 0x4E
                && b[3] == 0x47
                && b[4] == 0x0D
                && b[5] == 0x0A
                && b[6] == 0x1A
                && b[7] == 0x0A;
    }

    private boolean isJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF;
    }

    private boolean isWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 0x52
                && b[1] == 0x49
                && b[2] == 0x46
                && b[3] == 0x46
                && b[8] == 0x57
                && b[9] == 0x45
                && b[10] == 0x42
                && b[11] == 0x50;
    }

    private boolean isGif(byte[] b) {
        return b.length >= 6
                && b[0] == 0x47
                && b[1] == 0x49
                && b[2] == 0x46
                && b[3] == 0x38
                && (b[4] == 0x39 || b[4] == 0x37)
                && b[5] == 0x61;
    }

    private boolean looksLikeSvg(byte[] bytes) {
        int n = Math.min(bytes.length, 2048);
        String head = new String(bytes, 0, n, StandardCharsets.UTF_8).toLowerCase();
        return head.contains("<svg") || head.contains("xmlns=\"http://www.w3.org/2000/svg\"") || head.contains("<!doctype svg");
    }
}
