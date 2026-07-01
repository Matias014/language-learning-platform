package com.languageschool.backend.entity;

public enum LlmStatus {
    ok,
    timeout,
    rate_limited,
    quota_exceeded,
    safety_block,
    invalid_request,
    server_error
}
