package com.nexus.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 디버그용 컨트롤러
 * nginx와 직접 요청의 헤더 비교를 위한 임시 엔드포인트
 *
 * 문제 해결 후 삭제 예정
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    /**
     * 요청 정보를 에코하는 디버그 엔드포인트
     * POST/GET 모두 지원
     */
    @RequestMapping(value = "/echo", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Map<String, Object>> echo(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 기본 요청 정보
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("method", request.getMethod());
        result.put("requestURI", request.getRequestURI());
        result.put("requestURL", request.getRequestURL().toString());
        result.put("queryString", request.getQueryString());
        result.put("protocol", request.getProtocol());
        result.put("scheme", request.getScheme());
        result.put("serverName", request.getServerName());
        result.put("serverPort", request.getServerPort());
        result.put("remoteAddr", request.getRemoteAddr());
        result.put("remoteHost", request.getRemoteHost());

        // 중요 헤더들
        Map<String, String> importantHeaders = new LinkedHashMap<>();
        importantHeaders.put("Host", request.getHeader("Host"));
        importantHeaders.put("Origin", request.getHeader("Origin"));
        importantHeaders.put("Content-Type", request.getHeader("Content-Type"));
        importantHeaders.put("Content-Length", request.getHeader("Content-Length"));
        importantHeaders.put("Transfer-Encoding", request.getHeader("Transfer-Encoding"));
        importantHeaders.put("Connection", request.getHeader("Connection"));
        importantHeaders.put("Upgrade", request.getHeader("Upgrade"));
        importantHeaders.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        importantHeaders.put("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
        importantHeaders.put("X-Forwarded-Host", request.getHeader("X-Forwarded-Host"));
        importantHeaders.put("X-Real-IP", request.getHeader("X-Real-IP"));
        importantHeaders.put("Authorization", request.getHeader("Authorization") != null ? "[PRESENT]" : null);
        result.put("importantHeaders", importantHeaders);

        // 모든 헤더
        Map<String, List<String>> allHeaders = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            List<String> values = new ArrayList<>();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String value = headerValues.nextElement();
                // Authorization 값은 마스킹
                if ("Authorization".equalsIgnoreCase(headerName)) {
                    values.add("[MASKED]");
                } else {
                    values.add(value);
                }
            }
            allHeaders.put(headerName, values);
        }
        result.put("allHeaders", allHeaders);

        // Body 정보 (길이만)
        int contentLength = request.getContentLength();
        result.put("contentLength", contentLength);

        // Body 읽기 시도 (POST인 경우)
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            try {
                StringBuilder body = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                int charCount = 0;
                while ((line = reader.readLine()) != null && charCount < 1000) {
                    body.append(line);
                    charCount += line.length();
                }
                result.put("bodyPreview", body.toString().substring(0, Math.min(body.length(), 200)));
                result.put("bodyReadSuccess", true);
            } catch (IOException e) {
                result.put("bodyReadError", e.getMessage());
                result.put("bodyReadSuccess", false);
            }
        }

        // 로그 출력
        log.info("=== DEBUG ECHO REQUEST ===");
        log.info("Method: {}", request.getMethod());
        log.info("URI: {}", request.getRequestURI());
        log.info("Host: {}", request.getHeader("Host"));
        log.info("Origin: {}", request.getHeader("Origin"));
        log.info("Content-Type: {}", request.getHeader("Content-Type"));
        log.info("Content-Length: {}", request.getHeader("Content-Length"));
        log.info("Connection: {}", request.getHeader("Connection"));
        log.info("Upgrade: {}", request.getHeader("Upgrade"));
        log.info("X-Forwarded-For: {}", request.getHeader("X-Forwarded-For"));
        log.info("X-Forwarded-Proto: {}", request.getHeader("X-Forwarded-Proto"));
        log.info("=========================");

        return ResponseEntity.ok(result);
    }
}
