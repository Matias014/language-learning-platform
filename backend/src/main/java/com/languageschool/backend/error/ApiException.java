package com.languageschool.backend.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public ApiException(HttpStatus status, ErrorCode code) {
        super(code != null ? code.name() : status.getReasonPhrase());
        this.status = status;
        this.code = code != null ? code : defaultCodeFor(status);
    }

    public ApiException(ErrorCode code) {
        this(HttpStatus.BAD_REQUEST, code);
    }

    public static ApiException badRequest(ErrorCode code) {
        return new ApiException(HttpStatus.BAD_REQUEST, code);
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND);
    }

    public static ApiException notFound(ErrorCode code) {
        return new ApiException(HttpStatus.NOT_FOUND, code);
    }

    public static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    }

    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    public static ApiException forbidden(ErrorCode code) {
        return new ApiException(HttpStatus.FORBIDDEN, code);
    }

    public static ApiException conflict(ErrorCode code) {
        return new ApiException(HttpStatus.CONFLICT, code);
    }

    public static ApiException tooManyRequests() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS);
    }

    public static ApiException serviceUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE);
    }

    public static ApiException internal() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    public static ApiException unprocessable(ErrorCode code) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code);
    }

    private static ErrorCode defaultCodeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
            case NOT_ACCEPTABLE -> ErrorCode.NOT_ACCEPTABLE;
            case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case CONFLICT -> ErrorCode.CONFLICT;
            case TOO_MANY_REQUESTS -> ErrorCode.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
            case UNPROCESSABLE_ENTITY -> ErrorCode.UNPROCESSABLE_ENTITY;
            case PAYLOAD_TOO_LARGE -> ErrorCode.FILE_TOO_LARGE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
