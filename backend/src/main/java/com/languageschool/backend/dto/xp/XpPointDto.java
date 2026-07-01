package com.languageschool.backend.dto.xp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class XpPointDto {
    private LocalDate date;
    private long xp;
}
