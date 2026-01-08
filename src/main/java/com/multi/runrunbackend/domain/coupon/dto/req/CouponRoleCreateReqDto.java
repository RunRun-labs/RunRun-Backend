package com.multi.runrunbackend.domain.coupon.dto.req;

import com.multi.runrunbackend.domain.coupon.constant.CouponTriggerEvent;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponRoleCreateReqDto
 * @since : 2025. 12. 29. Monday
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRoleCreateReqDto {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 최대 50자까지 가능합니다.")
    private String name;

    @NotNull(message = "couponId는 필수입니다.")
    @Positive(message = "couponId는 양수여야 합니다.")
    private Long couponId;

    @NotNull(message = "triggerEvent는 필수입니다.")
    private CouponTriggerEvent triggerEvent;


    private Integer conditionValue;

    @AssertTrue(message = "RUN_COUNT_REACHED는 conditionValue(1 이상)가 필요합니다.")
    public boolean isConditionValueValid() {
        if (triggerEvent == null) {
            return true;
        }
        if (triggerEvent == CouponTriggerEvent.RUN_COUNT_REACHED) {
            return conditionValue != null && conditionValue > 0;
        }
        return true;
    }
}
