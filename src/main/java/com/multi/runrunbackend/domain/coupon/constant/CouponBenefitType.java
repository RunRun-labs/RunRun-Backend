package com.multi.runrunbackend.domain.coupon.constant;

/**
 * @author : kyungsoo
 * @description : 쿠폰이 제공하는 혜택의 유형을 정의하는 Enum. 할인, 서비스 내 체험, 제휴사 교환권 등 쿠폰의 성격을 구분한다.
 * @filename : CouponBenefitType
 * @since : 2025. 12. 17. Wednesday
 */
public enum CouponBenefitType {
    FIXED_DISCOUNT,   // 정액 할인 (원)
    RATE_DISCOUNT,    // 정률 할인 (%)
    EXPERIENCE,    // 우리 서비스 내 체험형 혜택
    VOUCHER        // 교환권 / 제휴사 쿠폰 / 바코드 형태
}
