package com.multi.runrunbackend.domain.term.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 약관 타입 관리 Enum (서비스 이용약관, 개인정보처리방침, 마케팅 수신동의)
 * @filename : TermsType
 * @since : 25. 12. 17. 오후 1:24 수요일
 */
@Getter
@RequiredArgsConstructor
public enum TermsType {
    SERVICE("서비스 이용약관"),
    PRIVACY("개인정보 처리방침"),
    MARKETING("마케팅 정보 수신 동의");

    private final String description;
}
