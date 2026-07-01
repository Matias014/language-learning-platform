package com.languageschool.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeEmailRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String newEmail;

    @NotBlank
    @Size(min = 8, max = 255)
    private String currentPassword;
}
