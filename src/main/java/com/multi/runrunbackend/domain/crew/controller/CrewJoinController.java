package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.dto.req.CrewJoinReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewJoinRequestResDto;
import com.multi.runrunbackend.domain.crew.service.CrewJoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 가입 관련 Controller
 * @filename : CrewJoinController
 * @since : 25. 12. 18. 목요일
 */
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
@Slf4j
public class CrewJoinController {

    private final CrewJoinService crewJoinService;

    /**
     * @param crewId    가입 신청할 크루 ID
     * @param principal
     * @param reqDto    가입 신청 정보 (자기소개, 거리, 페이스, 지역)
     * @description : 회원이 크루에 가입 신청 (100P가 차감되며, 크루장에게 알림 발송)
     */
    @PostMapping("/{crewId}/join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long crewId,
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody CrewJoinReqDto reqDto
    ) {
        crewJoinService.requestJoin(crewId, principal, reqDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successNoData("크루 가입 신청이 완료되었습니다."));
    }

    /**
     * @param crewId    크루 ID
     * @param principal
     * @description : 신청자가 대기중인 가입 신청을 취소 (차감된 100P가 환불)
     */
    @DeleteMapping("/{crewId}/join-cancel")
    @Operation(summary = "가입 신청 취소", description = "대기중인 가입 신청을 취소하고 포인트를 환불받습니다.")
    public ResponseEntity<ApiResponse<Void>> cancelJoinRequest(
            @Parameter(description = "크루 ID", required = true)
            @PathVariable Long crewId,

            @AuthenticationPrincipal CustomUser principal
    ) {
        crewJoinService.cancelJoinRequest(crewId, principal);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successNoData("가입 신청이 취소되었습니다."));
    }

    /**
     * @param crewId    크루 ID
     * @param principal
     * @description : 크루장이 대기중인 가입 신청 목록을 조회 (크루장 또는 부크루장만 조회 가능)
     */
    @GetMapping("/{crewId}/join-requests")
    @Operation(summary = "가입 신청 목록 조회", description = "크루장이 대기중인 가입 신청 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<CrewJoinRequestResDto>>> getJoinRequestList(
            @Parameter(description = "크루 ID", required = true)
            @PathVariable Long crewId,

            @AuthenticationPrincipal CustomUser principal
    ) {
        List<CrewJoinRequestResDto> joinRequests =
                crewJoinService.getJoinRequestList(crewId, principal);

        return ResponseEntity.ok(ApiResponse.success("가입 신청 목록을 조회했습니다.", joinRequests));
    }

    /**
     * @param crewId        크루 ID
     * @param joinRequestId 승인할 가입 신청 ID
     * @param principal
     * @description : 크루장이 가입 신청을 승인 (크루원으로 추가)
     */
    @PostMapping("/{crewId}/join-requests/{joinRequestId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveJoinRequest(
            @Parameter(description = "크루 ID", required = true)
            @PathVariable Long crewId,

            @Parameter(description = "가입 신청 ID", required = true)
            @PathVariable Long joinRequestId,

            @AuthenticationPrincipal CustomUser principal
    ) {
        crewJoinService.approveJoinRequest(crewId, principal, joinRequestId);

        return ResponseEntity.ok(
                ApiResponse.successNoData("가입 신청이 승인되었습니다.")
        );
    }

    /**
     * @param crewId        크루 ID
     * @param joinRequestId 거절할 가입 신청 ID
     * @param principal
     * @description : 크루장이 가입 신청을 거절 (포인트 환불)
     */
    @PostMapping("/{crewId}/join-requests/{joinRequestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoinRequest(
            @Parameter(description = "크루 ID", required = true)
            @PathVariable Long crewId,

            @Parameter(description = "가입 신청 ID", required = true)
            @PathVariable Long joinRequestId,

            @AuthenticationPrincipal CustomUser principal
    ) {
        crewJoinService.rejectJoinRequest(crewId, principal, joinRequestId);

        return ResponseEntity.ok(
                ApiResponse.successNoData("가입 신청이 거절되었습니다.")
        );
    }

    /**
     * @param crewId    크루 ID
     * @param principal
     * @description : 크루원이 크루 탈퇴
     */
    @DeleteMapping("/{crewId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveCrew(
            @Parameter(description = "크루 ID", required = true)
            @PathVariable Long crewId,

            @AuthenticationPrincipal CustomUser principal
    ) {
        crewJoinService.leaveCrew(crewId, principal);

        return ResponseEntity.ok(
                ApiResponse.successNoData("크루에서 탈퇴되었습니다.")
        );
    }

}
