package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.dto.res.CrewMainResDto;
import com.multi.runrunbackend.domain.crew.service.CrewMainService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crews/main")
@RequiredArgsConstructor
public class CrewMainController {

    private final CrewMainService crewMainService;

    /**
     * @description : 크루 메인 화면 정보 조회 (내가 가입한 크루 정보)
     */
    @GetMapping
    @Operation(summary = "크루 메인 화면 조회", description = "로그인한 사용자의 크루 가입 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<CrewMainResDto>> getCrewMainInfo(
            @AuthenticationPrincipal CustomUser principal
    ) {
        CrewMainResDto response = crewMainService.getCrewMainInfo(principal);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("크루 메인 정보 조회 성공", response));
    }
}
