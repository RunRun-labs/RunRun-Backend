package com.multi.runrunbackend.domain.membership.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.membership.dto.res.MembershipMainResDto;
import com.multi.runrunbackend.domain.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : BoKyung
 * @description : 멤버십 관리 컨트롤러
 * @filename : MembershipController
 * @since : 25. 12. 30. 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    /**
     * 멤버십 메인 조회 (GET /api/memberships)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MembershipMainResDto>> getMembership(
            @AuthenticationPrincipal CustomUser principal
    ) {
        MembershipMainResDto res = membershipService.getMembership(principal);

        return ResponseEntity.ok(
                ApiResponse.success("멤버십 조회 성공", res)
        );
    }
}
