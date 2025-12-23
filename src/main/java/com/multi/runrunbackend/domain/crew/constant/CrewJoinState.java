package com.multi.runrunbackend.domain.crew.constant;

/**
 * @author : BoKyung
 * @description : 사용자 관점의 크루 가입 상태 ENUM (NOT_APPLIED: 신청 전, PENDING: 대기중, APPROVED: 승인됨, CAN_REAPPLY: 재신청 가능)
 * @filename : CrewJoinState
 * @since : 25. 12. 23. 화요일
 */
public enum CrewJoinState {

    NOT_APPLIED,
    PENDING,
    APPROVED,
    CAN_REAPPLY
}
