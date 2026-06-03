package com.KhoiCG.TMDT.common.exception;

import com.KhoiCG.TMDT.common.config.RequestCorrelationFilter;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwt(ExpiredJwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "TOKEN_EXPIRED", "message", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<?> handleMissingCookie(MissingRequestCookieException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "UNAUTHORIZED", "message", "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "INVALID_CREDENTIALS", "message", "Email hoặc mật khẩu không chính xác."));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AuthorizationDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "FORBIDDEN", "message", "Bạn không có quyền truy cập tài nguyên này."));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "VALIDATION_ERROR",
                        "message", "Dữ liệu đầu vào không hợp lệ.",
                        "errors", fieldErrors
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "MISSING_REQUEST_PARAMETER",
                        "message", "Thiếu tham số bắt buộc: " + e.getParameterName()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableMessage(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "MALFORMED_REQUEST_BODY",
                        "message", "Nội dung request body không hợp lệ."
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of(
                        "code", "METHOD_NOT_ALLOWED",
                        "message", "Phương thức HTTP không được hỗ trợ cho endpoint này."
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestId = String.valueOf(request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTR));
        log.error("RuntimeException requestId={} uri={}", requestId, request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "BAD_REQUEST", "message", "Yêu cầu không hợp lệ."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnhandledException(Exception e, HttpServletRequest request) {
        String requestId = String.valueOf(request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTR));
        log.error("Unhandled error requestId={}", requestId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "INTERNAL_SERVER_ERROR",
                        "message", "Hệ thống gặp lỗi, vui lòng thử lại sau.",
                        "requestId", requestId
                ));
    }
}