package com.multi.runrunbackend.domain.payment.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CARD,  // 카드
    TOSSPAY, // 토스페이
    VIRTUAL_ACCOUNT,  // 가상계좌
    TRANSFER,  // 계좌이체
    MOBILE_PHONE,  // 휴대폰
    CULTURE_GIFT_CERTIFICATE,  // 문화상품권
    BOOK_CULTURE_GIFT_CERTIFICATE  // 도서문화상품권
}
