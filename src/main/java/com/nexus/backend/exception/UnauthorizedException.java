package com.nexus.backend.exception;

/**
 * 인증되지 않았거나 권한이 없을 때 발생하는 예외
 * HTTP 401 Unauthorized / 403 Forbidden
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
