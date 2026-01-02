package com.multi.runrunbackend.domain.match.constant;

/**
 * @author : KIMGWANGHO
 * @description : 매치 세션 진행 상태 관리 Enum (대기중, 진행중, 종료됨, 취소됨)
 * @filename : SessionStatus
 * @since : 2025-12-17 수요일
 */
public enum SessionStatus {
    STANDBY, IN_PROGRESS, COMPLETED, CANCELLED
}