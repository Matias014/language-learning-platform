package com.languageschool.backend.dto.userSrs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrsSummaryDto {
    private long dueTodayCount;
    private long dueNext7DaysCount;
}
