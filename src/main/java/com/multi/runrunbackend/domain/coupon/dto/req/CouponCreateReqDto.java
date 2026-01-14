package com.multi.runrunbackend.domain.coupon.dto.req;

import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponCreateReqdto
 * @since : 2025. 12. 27. Saturday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateReqDto {

    @NotBlank(message = "쿠폰명은 필수입니다.")
    @Size(min = 5, max = 50, message = "쿠폰명은 5~50자여야 합니다.")
    private String name;

    @NotBlank(message = "쿠폰 설명은 필수입니다.")
    @Size(min = 10, max = 500, message = "쿠폰 설명은 10~500자여야 합니다.")
    private String description;

    @NotNull(message = "발급 수량(quantity)은 필수입니다.")
    @Min(value = 0, message = "발급 수량(quantity)은 0 이상이어야 합니다.")
    private Integer quantity;

    @NotNull(message = "쿠폰 코드 타입(codeType)은 필수입니다. (SINGLE/MULTI)")
    private CouponCodeType codeType;

    @NotNull(message = "발급 채널(channel)은 필수입니다.")
    private CouponChannel channel;

    @NotNull(message = "혜택 타입(benefitType)은 필수입니다.")
    private CouponBenefitType benefitType;

    @NotNull(message = "혜택 값(benefitValue)은 필수입니다.")
    @Positive(message = "혜택 값(benefitValue)은 양수여야 합니다.")
    private Integer benefitValue;

    @Size(min = 6, max = 20, message = "쿠폰 코드(code)는 6~20자여야 합니다.")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "쿠폰 코드(code)는 영문 대문자(A-Z)와 숫자(0-9)만 허용합니다.")
    private String code;

    @NotNull(message = "쿠폰 시작일(startAt)은 필수입니다.")
    @Future(message = "쿠폰 시작일(startAt)은 현재 시각 이후여야 합니다.")
    private LocalDateTime startAt;

    @NotNull(message = "쿠폰 종료일(endAt)은 필수입니다.")
    @Future(message = "쿠폰 종료일(endAt)은 현재 시각 이후여야 합니다.")
    private LocalDateTime endAt;

    @AssertTrue(message = "쿠폰 종료일(endAt)은 쿠폰 시작일(startAt) 이후여야 합니다.")
    public boolean isValidPeriod() {
        if (startAt == null || endAt == null) {
            return true; // null 검증은 @NotNull이 담당
        }
        return endAt.isAfter(startAt);
    }

    @AssertTrue(message = "쿠폰 시작일/종료일(startAt/endAt)은 00:00:00 이어야 합니다.")
    public boolean isMidnight() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return isExactlyMidnight(startAt) && isExactlyMidnight(endAt);
    }

    private boolean isExactlyMidnight(LocalDateTime t) {
        return t.getHour() == 0 && t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

    @AssertTrue(message = "정률 할인은 1~100%만 가능합니다.")
    private boolean isValidRateDiscount() {
        if (benefitType != CouponBenefitType.RATE_DISCOUNT) {
            return true;
        }
        if (benefitValue == null) {
            return false;
        }
        return benefitValue >= 1 && benefitValue <= 100;
    }

    @AssertTrue(message = "정액 할인은 1000~9900원, 1000원 단위만 가능합니다.")
    private boolean isValidFixedDiscount() {
        if (benefitType != CouponBenefitType.FIXED_DISCOUNT) {
            return true;
        }
        if (benefitValue == null) {
            return false;
        }
        if (benefitValue < 1000 || benefitValue > 9900) {
            return false;
        }
        return benefitValue % 1000 == 0;
    }

    @AssertTrue(message = "MULTI 쿠폰은 쿠폰 코드(code)가 필수입니다.")
    public boolean isCodeRequiredForMulti() {
        if (codeType == null) {
            return true;
        }
        if (codeType == CouponCodeType.MULTI) {
            return code != null && !code.trim().isEmpty();
        }
        return true; // SINGLE이면 code 없어도 OK
    }

    @AssertTrue(message = "쿠폰 코드(code)는 공백을 포함할 수 없습니다.")
    public boolean isCodeNoSpaces() {
        if (code == null) {
            return true;
        }
        return !code.contains(" ");
    }
}