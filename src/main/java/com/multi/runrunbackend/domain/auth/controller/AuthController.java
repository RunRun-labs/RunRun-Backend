package com.multi.runrunbackend.domain.auth.controller;

import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.service.TokenService;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.AuthSignInResDto;
import com.multi.runrunbackend.domain.auth.service.AuthService;
import com.multi.runrunbackend.domain.user.dto.req.UserSignInDto;
import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@RequestBody @Valid UserSignUpDto userSignUpDto) {
        authService.signUp(userSignUpDto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("회원가입 성공", null));

    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody UserSignInDto userSignInDto) {

        AuthSignInResDto res = authService.login(userSignInDto);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success("로그인 성공", res));

    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(
        @RequestHeader("Authorization") String refreshToken) {
        TokenDto token = tokenService.createToken(refreshToken);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success("로그인 성공", token));
    }

    @GetMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String accessToken) {
        tokenService.registBlackList(accessToken);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success("로그아웃 성공", null));
    }


}
