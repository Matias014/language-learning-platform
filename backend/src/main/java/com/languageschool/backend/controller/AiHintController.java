package com.languageschool.backend.controller;

import com.languageschool.backend.dto.ai.HintDtos.HintRequest;
import com.languageschool.backend.dto.ai.HintDtos.HintResponse;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.AiHintService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AiHintController {

    private final ObjectProvider<AiHintService> hintServiceProvider;

    public AiHintController(ObjectProvider<AiHintService> hintServiceProvider) {
        this.hintServiceProvider = hintServiceProvider;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/ai/hints")
    public HintResponse hint(@Valid @RequestBody HintRequest request, Authentication authentication) {
        AiHintService service = hintServiceProvider.getIfAvailable();
        if (service == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.AI_DISABLED);
        }
        String login = authentication != null ? authentication.getName() : null;
        if (login == null || login.isBlank()) {
            throw ApiException.unauthorized();
        }
        return service.hint(request, login);
    }
}
