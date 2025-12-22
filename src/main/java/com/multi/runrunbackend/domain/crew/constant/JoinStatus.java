package com.multi.runrunbackend.domain.crew.constant;

/**
 * @author : BoKyung
 * @description : 크루 가입 신청 상태 ENUM
 * @filename : JoinStatus
 * @since : 25. 12. 22. 월요일
 */
public enum JoinStatus {
    PENDING,    // 대기중
    APPROVED,   // 승인됨
    REJECTED,   // 거절됨
    CANCELED    // 취소됨

}
