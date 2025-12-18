package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.dto.req.CrewCreateReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewUpdateReqDto;
import com.multi.runrunbackend.domain.crew.service.CrewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @author : BoKyung
 * @description : 크루 Controller
 * @filename : CrewController
 * @since : 25. 12. 18. 목요일
 */
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
@Tag(name = "크루 API")
public class CrewController {

    private final CrewService crewService;

    /**
     * @param reqDto 크루 생성 요청 DTO
     * @description : 크루 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCrew(
            @AuthenticationPrincipal CustomUser customUser,
            @Valid @RequestBody CrewCreateReqDto reqDto
    ) {
        String loginId = customUser.getEmail();
        Long crewId = crewService.createCrew(loginId, reqDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("크루 생성 성공", crewId));
    }

    /**
     * @param customUser 인증된 사용자 정보
     * @param crewId     크루 ID
     * @param reqDto     크루 수정 요청 DTO
     * @description : 크루 수정
     */
    @PutMapping("/{crewId}")
    public ResponseEntity<ApiResponse<Void>> updateCrew(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long crewId,
            @Valid @RequestBody CrewUpdateReqDto reqDto
    ) {
        String loginId = customUser.getEmail();
        crewService.updateCrew(crewId, loginId, reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("크루 정보 수정 성공", null));
    }
}
