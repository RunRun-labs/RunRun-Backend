package com.multi.runrunbackend.domain.payment.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : BoKyung
 * @description : 결제 상태
 * @filename : PaymentStatus
 * @since : 2026. 1. 1.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    READY,          // 결제 생성됨
    DONE,              // 결제 완료
    FAILED,       // 결제 실패
    CANCELED     // 시스템 취소

}
