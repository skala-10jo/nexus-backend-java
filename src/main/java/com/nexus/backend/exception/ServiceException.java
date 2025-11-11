package com.nexus.backend.exception;

/**
 * 서비스 로직 실행 중 발생하는 예외
 * HTTP 500 Internal Server Error
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
