package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserSettingReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserSettingResDto;
import com.multi.runrunbackend.domain.user.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author : kimyongwon
 * @description : 사용자의 개인 설정을 조회/수정하기 위한 REST 컨트롤러.
 * @filename : UserSettingController
 * @since : 25. 12. 28. 오후 10:37 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/settings")
public class UserSettingController {

    private final UserSettingService userSettingService;


    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingResDto>> getUserSetting(
            @AuthenticationPrincipal CustomUser principal
    ) {
        UserSettingResDto res = userSettingService.getUserSetting(principal);
        return ResponseEntity.ok(ApiResponse.success("사용자 설정 조회 성공", res));
    }


    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateUserSetting(
            @RequestBody UserSettingReqDto req,
            @AuthenticationPrincipal CustomUser principal
    ) {
        userSettingService.updateUserSetting(req, principal);
        return ResponseEntity.ok(ApiResponse.success("사용자 설정 수정 성공", null));
    }
}