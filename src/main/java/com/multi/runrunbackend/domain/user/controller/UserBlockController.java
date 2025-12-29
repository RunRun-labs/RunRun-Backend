package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.res.UserBlockResDto;
import com.multi.runrunbackend.domain.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description :  사용자 차단, 차단 해제, 차단 목록 조회 컨트롤러
 * @filename : UserBlockController
 * @since : 25. 12. 29. 오전 12:21 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/blocks")
public class UserBlockController {

    private final UserBlockService userBlockService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<UserBlockResDto>>> getBlockedUsers(
            @AuthenticationPrincipal CustomUser principal
    ) {
        List<UserBlockResDto> res = userBlockService.getBlockedUsers(principal);
        return ResponseEntity.ok(ApiResponse.success("차단 목록 조회 성공", res));
    }

    @PostMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        userBlockService.blockUser(targetUserId, principal);
        return ResponseEntity.ok(ApiResponse.success("사용자 차단 성공", null));
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        userBlockService.unblockUser(targetUserId, principal);
        return ResponseEntity.ok(ApiResponse.success("차단 해제 성공", null));
    }
}