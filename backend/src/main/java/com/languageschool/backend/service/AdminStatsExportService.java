package com.languageschool.backend.service;

import java.util.Locale;

public interface AdminStatsExportService {
    byte[] buildLlmStatsCsv(Locale locale);

    byte[] buildHardestExercisesCsv(int limit, Locale locale);

    byte[] buildLlmStatsPdf(Locale locale);

    byte[] buildHardestExercisesPdf(int limit, Locale locale);
}
