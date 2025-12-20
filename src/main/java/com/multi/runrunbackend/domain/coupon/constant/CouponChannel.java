package com.multi.runrunbackend.domain.coupon.constant;

/**
 * @author : kyungsoo
 * @description : 쿠폰이 어떤 경로를 통해 발급되었는지를 구분하기 위한 Enum. 이벤트, 시스템 자동 발급, 제휴사, 관리자 지급, 마케팅 프로모션 등 쿠폰의 발급
 * 출처를 명확히 관리하기 위해 사용된다.
 * @filename : CouponChannel
 * @since : 2025. 12. 17. Wednesday
 */
public enum CouponChannel {

    /**
     * 이벤트 참여를 통해 발급되는 쿠폰 예: 출석 이벤트, 러닝 챌린지, 오프라인 행사 보상
     */
    EVENT,

    /**
     * 시스템 로직에 의해 자동 발급되는 쿠폰 예: 회원가입, 첫 러닝, 러닝 횟수 달성, 생일 쿠폰
     */
    SYSTEM,

    /**
     * 외부 제휴사를 통해 제공되는 쿠폰 또는 교환권 예: 스타벅스 교환권, 나이키 할인 쿠폰
     */
    PARTNER,

    /**
     * 운영자(Admin)가 수동으로 지급하는 쿠폰 예: 장애 보상, 고객 CS 대응, 테스트용 지급
     */
    ADMIN,

    /**
     * 마케팅/프로모션 캠페인을 통해 발급되는 쿠폰 예: 광고 유입 보상, 푸시/이메일 복귀 쿠폰
     */
    PROMOTION
}