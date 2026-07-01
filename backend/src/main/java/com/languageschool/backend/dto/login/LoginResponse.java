package com.languageschool.backend.dto.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class LoginResponse {
    String accessToken;
    String tokenType;
    Long expiresIn;
}
