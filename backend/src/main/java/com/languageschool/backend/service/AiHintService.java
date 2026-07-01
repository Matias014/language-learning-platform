package com.languageschool.backend.service;

import com.languageschool.backend.dto.ai.HintDtos.HintRequest;
import com.languageschool.backend.dto.ai.HintDtos.HintResponse;

public interface AiHintService {

    HintResponse hint(HintRequest request, String login);
}
