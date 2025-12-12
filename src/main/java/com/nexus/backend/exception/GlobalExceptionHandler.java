package com.nexus.backend.exception;

import com.nexus.backend.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 모든 Controller에서 발생하는 예외를 처리하고 일관된 응답 형식 제공
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 리소스를 찾을 수 없을 때 (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 인증/권한 오류 (401)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 잘못된 요청 (400)
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 리소스 충돌 (409)
     * 이미 존재하는 리소스 생성 시도 등
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflictException(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 서비스 로직 오류 (500)
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleServiceException(ServiceException ex) {
        log.error("Service error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 유효성 검증 실패 (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> errorDetail = new HashMap<>();
                    errorDetail.put("field", error.getField());
                    errorDetail.put("message", error.getDefaultMessage());
                    errorDetail.put("value", error.getRejectedValue() != null ?
                            error.getRejectedValue().toString() : null);
                    return errorDetail;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("errors", errors);

        log.warn("Validation failed: {} errors", errors.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(response)
                        .build());
    }

    /**
     * 처리되지 않은 RuntimeException (400)
     * CustomException을 사용하지 않은 경우 대비
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 모든 예외의 최종 처리 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}
