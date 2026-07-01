package com.languageschool.backend.dto.userSrs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrsThisWeekDto {
    private int dueTotal;
    private List<DayCountDto> byDay;
}
