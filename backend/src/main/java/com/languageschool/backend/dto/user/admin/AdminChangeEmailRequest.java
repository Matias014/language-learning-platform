package com.languageschool.backend.dto.user.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminChangeEmailRequest {
    @NotBlank
    @Email
    @Size(max = 255)
    private String newEmail;
}
