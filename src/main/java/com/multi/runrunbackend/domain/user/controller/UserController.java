package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author : kimyongwon
 * @description :
 * 인증된 사용자의 계정 및 프로필 정보를 조회/수정하기 위한 REST 컨트롤러.
 * 사용자 정보 API를 제공한다.
 * 주요 기능:
 * - 내 정보 조회 (/users/me)
 * - 내 정보 수정 (/users/me)
 * @filename : UserController
 * @since : 25. 12. 18. 오후 4:22 목요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public UserResDto getUser(
            @AuthenticationPrincipal CustomUser principal
    ) {
        return userService.getUser(principal);
    }

    /**
     * 내 정보 수정
     */
    @PutMapping("/me")
    public void updateUser(
            @RequestBody @Valid UserUpdateReqDto req,
            @AuthenticationPrincipal CustomUser principal
    ) {
        userService.updateUser(req, principal);
    }
}