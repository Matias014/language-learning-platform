package com.languageschool.backend.dto.user;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String surname;
}
