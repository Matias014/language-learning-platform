package com.languageschool.backend.controller;

import com.languageschool.backend.service.AdminStatsExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsExportController {

    private static final String HDR_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private final AdminStatsExportService export;

    public AdminStatsExportController(AdminStatsExportService export) {
        this.export = export;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/llm/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportLlm(Locale requestLocale) {
        byte[] csv = export.buildLlmStatsCsv(requestLocale);
        String base = "llm-stats-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        String ascii = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String encoded = UriUtils.encode(base, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        headers.add(HDR_X_CONTENT_TYPE_OPTIONS, "nosniff");
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/exercises/hardest/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportHardest(@RequestParam(name = "limit", defaultValue = "10") int limit,
                                                Locale requestLocale) {
        byte[] csv = export.buildHardestExercisesCsv(limit, requestLocale);
        String base = "hardest-exercises-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        String ascii = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String encoded = UriUtils.encode(base, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        headers.add(HDR_X_CONTENT_TYPE_OPTIONS, "nosniff");
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/llm/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportLlmPdf(Locale requestLocale) {
        byte[] pdf = export.buildLlmStatsPdf(requestLocale);
        String base = "llm-stats-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";
        String ascii = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String encoded = UriUtils.encode(base, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        headers.add(HDR_X_CONTENT_TYPE_OPTIONS, "nosniff");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/exercises/hardest/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportHardestPdf(@RequestParam(name = "limit", defaultValue = "10") int limit,
                                                   Locale requestLocale) {
        byte[] pdf = export.buildHardestExercisesPdf(limit, requestLocale);
        String base = "hardest-exercises-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";
        String ascii = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String encoded = UriUtils.encode(base, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        headers.add(HDR_X_CONTENT_TYPE_OPTIONS, "nosniff");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
