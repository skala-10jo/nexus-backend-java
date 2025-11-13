package com.nexus.backend.entity;

public enum DocumentStatus {
    UPLOADED,    // 업로드 완료
    PROCESSING,  // 분석 중
    PROCESSED,   // 분석 완료 (사용 가능)
    FAILED       // 오류 발생
}
