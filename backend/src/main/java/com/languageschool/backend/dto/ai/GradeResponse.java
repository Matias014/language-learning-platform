package com.languageschool.backend.dto.ai;

import java.util.List;

public record GradeResponse(boolean correct, String feedback, List<String> hints,
                            Long attemptId, Integer awardedXp) {
}
