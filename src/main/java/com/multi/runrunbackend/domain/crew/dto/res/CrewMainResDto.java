package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewMainResDto {

    // 크루 가입 여부
    private Boolean hasJoinedCrew;

    // 가입한 크루 정보 (가입한 경우에만)
    private Long crewId;
    private String crewName;
    private CrewRole role;

    // 권한 정보
    private Boolean isLeader;  // 크루장 여부
    private Boolean canManageJoinRequests;  // 가입 신청 관리 권한 (크루장 + 부크루장)
}
