package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.AttendanceCheckResDto;
import com.multi.runrunbackend.domain.user.dto.res.AttendanceStatusResDto;
import com.multi.runrunbackend.domain.user.dto.res.UserProfileResDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author : kimyongwon
 * @description :
 * 인증된 사용자의 계정 및 프로필 정보를 조회/수정하기 위한 REST 컨트롤러.
 * 사용자 정보 API를 제공한다.
 * 주요 기능:
 * - 내 정보 조회 (/users)
 * - 내 정보 수정 (/users)
 * @filename : UserController
 * @since : 25. 12. 18. 오후 4:22 목요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;


    @GetMapping
    public ResponseEntity<ApiResponse<UserResDto>> getUser(
            @AuthenticationPrincipal CustomUser principal
    ) {
        UserResDto res = userService.getUser(principal);
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 성공", res));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResDto>> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        UserProfileResDto res = userService.getUserProfile(userId, principal);
        return ResponseEntity.ok(
                ApiResponse.success("사용자 프로필 조회 성공", res)
        );
    }

    @PutMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse> updateUser(

            @RequestPart(value = "request") @Valid UserUpdateReqDto req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUser principal
    ) {

        userService.updateUser(req, file, principal);
        return ResponseEntity.ok(ApiResponse.success("프로필 수정 성공", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUser principal
    ) {
        userService.deleteUser(principal);
        return ResponseEntity.ok(
                ApiResponse.success("회원 탈퇴가 완료되었습니다.", null)
        );
    }

    /**
     * 출석 체크 API
     */
    @PostMapping("/attendance")
    @ResponseBody
    public ResponseEntity<AttendanceCheckResDto> checkAttendance(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        AttendanceCheckResDto result = userService.checkAttendance(customUser.getUserId());
        return ResponseEntity.ok(result);
    }

    /**
     * 출석 현황 조회 API
     */
    @GetMapping("/attendance/status")
    @ResponseBody
    public ResponseEntity<AttendanceStatusResDto> getAttendanceStatus(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        AttendanceStatusResDto result = userService.getAttendanceStatus(customUser.getUserId());
        return ResponseEntity.ok(result);
    }

}
