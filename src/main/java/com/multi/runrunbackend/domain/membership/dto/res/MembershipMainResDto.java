package com.multi.runrunbackend.domain.membership.dto.res;

import com.multi.runrunbackend.domain.membership.entity.Membership;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 멤버십 메인 조회 응답 DTO
 * @filename : MembershipMainResDto
 * @since : 25. 12. 30. 월요일
 */
@Getter
@Builder
public class MembershipMainResDto {

    private String membershipGrade;      // 멤버십 등급
    private String membershipStatus;     // 멤버십 상태 (사용중, 해지 신청됨, 만료됨)
    private LocalDateTime startDate;     // 시작일
    private LocalDateTime endDate;       // 종료일
    private LocalDateTime nextBillingDate; // 다음 결제일

    public static MembershipMainResDto fromEntity(Membership membership) {
        return MembershipMainResDto.builder()
                .membershipGrade(membership.getMembershipGrade())
                .membershipStatus(membership.getMembershipStatus())
                .startDate(membership.getStartDate())
                .endDate(membership.getEndDate())
                .nextBillingDate(membership.getNextBillingDate())
                .build();
    }
}
