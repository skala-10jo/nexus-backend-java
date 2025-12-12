package com.nexus.backend.exception;

/**
 * 리소스 충돌 시 발생하는 예외 (이미 존재하는 리소스)
 * HTTP 409 Conflict
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
