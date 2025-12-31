package com.multi.runrunbackend.domain.membership.dto.res;

import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 멤버십 메인 조회 응답 DTO
 * @filename : MembershipMainResDto
 * @since : 25. 12. 30. 월요일
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MembershipMainResDto {

    private MembershipStatus membershipStatus;     // 멤버십 상태 (사용중, 해지 신청됨, 만료됨)
    private LocalDateTime startDate;     // 시작일
    private LocalDateTime endDate;       // 종료일 (해지 신청할때 사용할 예정)
    private LocalDateTime nextBillingDate; // 다음 결제일

    /**
     * @description : Entity를 DTO로 변환
     */
    public static MembershipMainResDto fromEntity(Membership membership) {
        return MembershipMainResDto.builder()
                .membershipStatus(membership.getMembershipStatus())
                .startDate(membership.getStartDate())
                .endDate(membership.getEndDate())
                .nextBillingDate(membership.getNextBillingDate())
                .build();
    }
}
