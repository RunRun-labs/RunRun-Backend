package com.multi.runrunbackend.domain.membership.constant;

/**
 * @author : BoKyung
 * @description : 멤버십 상태를 관리하는 열거형 (Enum) - ACTIVE(활성화), CANCELED(해지신청), EXPIRED(만료)
 * @filename : MembershipStatus
 * @since : 25. 12. 30. 월요일
 */
public enum MembershipStatus {

    ACTIVE,      // 활성화 (사용 중)
    CANCELED,    // 해지 신청됨
    EXPIRED      // 만료됨
}
