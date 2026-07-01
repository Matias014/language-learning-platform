package com.languageschool.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeLoginRequest {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,64}$",
            message = "Login może zawierać tylko litery/cyfry/kropki/podkreślenia/myślniki")
    private String newLogin;

    @NotBlank
    @Size(min = 8, max = 255)
    private String currentPassword;
}
