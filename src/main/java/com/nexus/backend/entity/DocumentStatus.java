package com.nexus.backend.entity;

public enum DocumentStatus {
    UPLOADED,    // 업로드 완료
    PROCESSING,  // 분석 중
    READY,       // 사용 가능
    ERROR        // 오류 발생
}
