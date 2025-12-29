package com.multi.runrunbackend.domain.coupon.dto.req;

import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponCodeType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * @filename : CouponUpdateReqdto
 * @since : 2025. 12. 29. Monday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUpdateReqDto {


    @NotBlank
    @Size(min = 5, max = 50)
    private String name;

    @NotBlank
    @Size(min = 10, max = 500)
    private String description;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotNull
    private CouponCodeType codeType;

    @NotNull
    private CouponChannel channel;

    @NotNull
    private CouponBenefitType benefitType;

    @NotNull
    @Positive
    private Integer benefitValue;


    @NotNull
    @Future(message = "startAt은 현재 시각 이후여야 합니다.")
    private LocalDateTime startAt;

    @NotNull
    @Future(message = "endAt은 현재 시각 이후여야 합니다.")
    private LocalDateTime endAt;

    @AssertTrue(message = "endAt은 startAt 이후여야 합니다.")
    public boolean isValidPeriod() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return endAt.isAfter(startAt);
    }

    @AssertTrue(message = "startAt/endAt은 00:00:00 이어야 합니다.")
    public boolean isMidnight() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return isExactlyMidnight(startAt) && isExactlyMidnight(endAt);
    }

    private boolean isExactlyMidnight(LocalDateTime t) {
        return t.getHour() == 0 && t.getMinute() == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

}
