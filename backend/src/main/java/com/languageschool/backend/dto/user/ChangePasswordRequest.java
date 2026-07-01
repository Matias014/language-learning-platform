package com.languageschool.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank
    @Size(min = 8, max = 255)
    private String newPassword;

    @NotBlank
    @Size(min = 8, max = 255)
    private String currentPassword;
}
