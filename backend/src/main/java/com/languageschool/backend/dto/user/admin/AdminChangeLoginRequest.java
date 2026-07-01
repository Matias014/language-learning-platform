package com.languageschool.backend.dto.user.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminChangeLoginRequest {
    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,64}$")
    private String newLogin;
}
