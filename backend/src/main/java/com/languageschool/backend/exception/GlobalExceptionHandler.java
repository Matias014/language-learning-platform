package com.languageschool.backend.exception;

import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatus());
        pd.setTitle(ex.getStatus().getReasonPhrase());
        pd.setDetail(ex.getCode().name());
        pd.setProperty("code", ex.getCode().name());
        enrich(pd, req);
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCred(BadCredentialsException ignored, HttpServletRequest req) {
        return handleApi(ApiException.unauthorized(), req);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ProblemDetail handleInsufficient(InsufficientAuthenticationException ignored, HttpServletRequest req) {
        return handleApi(ApiException.unauthorized(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ignored, HttpServletRequest req) {
        return handleApi(ApiException.forbidden(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setDetail("Validation failed");
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("fields", fields);
        pd.setProperty("code", ErrorCode.BAD_REQUEST.name());
        enrich(pd, req);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setDetail("Validation failed");
        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            fields.put(String.valueOf(v.getPropertyPath()), v.getMessage());
        }
        pd.setProperty("fields", fields);
        pd.setProperty("code", ErrorCode.BAD_REQUEST.name());
        enrich(pd, req);
        return pd;
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ProblemDetail handleTx(TransactionSystemException ex, HttpServletRequest req) {
        Throwable t = ex.getMostSpecificCause() == null ? ex : ex.getMostSpecificCause();
        while (t != null) {
            if (t instanceof ConstraintViolationException cve) {
                return handleConstraint(cve, req);
            }
            t = t.getCause();
        }
        return handleApi(ApiException.badRequest(ErrorCode.BAD_REQUEST), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConflict(DataIntegrityViolationException ignored, HttpServletRequest req) {
        return handleApi(ApiException.conflict(ErrorCode.CONFLICT), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ignored, HttpServletRequest req) {
        return handleApi(ApiException.badRequest(ErrorCode.BAD_REQUEST), req);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class
    })
    public ProblemDetail handleBadRequest(Exception ignored, HttpServletRequest req) {
        return handleApi(ApiException.badRequest(ErrorCode.BAD_REQUEST), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ignored, HttpServletRequest req) {
        return handleApi(new ApiException(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMedia(HttpMediaTypeNotSupportedException ignored, HttpServletRequest req) {
        return handleApi(new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE), req);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ProblemDetail handleNotAcceptable(HttpMediaTypeNotAcceptableException ignored, HttpServletRequest req) {
        return handleApi(new ApiException(HttpStatus.NOT_ACCEPTABLE, ErrorCode.NOT_ACCEPTABLE), req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ignored, HttpServletRequest req) {
        return handleApi(new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.FILE_TOO_LARGE), req);
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipart(MultipartException ignored, HttpServletRequest req) {
        return handleApi(ApiException.badRequest(ErrorCode.BAD_REQUEST), req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on path={}", req.getRequestURI(), ex);
        return handleApi(ApiException.internal(), req);
    }

    private void enrich(ProblemDetail pd, HttpServletRequest req) {
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("method", req.getMethod());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
    }
}
