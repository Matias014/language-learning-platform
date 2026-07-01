package com.languageschool.backend.service;

import com.languageschool.backend.dto.userSrs.SrsSummaryDto;

public interface SrsSummaryService {
    SrsSummaryDto getSummaryForLogin(String login);
}
