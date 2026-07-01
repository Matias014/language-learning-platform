package com.languageschool.backend.dto.exerciseAward;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExerciseAwardRequest {

    @NotNull
    private Long attemptId;

//    awardedXp XP powinien wyliczać BE (na bazie Exercise.xp, poprawności, ewentualnych mnożników)
}
