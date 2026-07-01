package com.languageschool.backend.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    @Size(max = 255)
    private String loginOrEmail;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;
}
