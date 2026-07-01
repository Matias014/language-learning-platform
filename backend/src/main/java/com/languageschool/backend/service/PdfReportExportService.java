package com.languageschool.backend.service;

import java.util.Locale;

public interface PdfReportExportService {
    byte[] buildUserReportForLogin(String login, Locale locale);
}
