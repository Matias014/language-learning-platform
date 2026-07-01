package com.languageschool.backend.controller;

import com.languageschool.backend.service.PdfReportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class UserExportController {

    private static final String HDR_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private final PdfReportExportService reports;

    public UserExportController(PdfReportExportService reports) {
        this.reports = reports;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/users/me/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportMe(@RequestParam(name = "lang", required = false) String lang,
                                           Locale requestLocale,
                                           Authentication auth) {
        Locale locale = (lang != null && !lang.isBlank()) ? Locale.forLanguageTag(lang) : requestLocale;
        byte[] pdf = reports.buildUserReportForLogin(auth.getName(), locale);
        String base = "export-" + auth.getName() + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";
        String ascii = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String encoded = UriUtils.encode(base, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        headers.add(HDR_X_CONTENT_TYPE_OPTIONS, "nosniff");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
