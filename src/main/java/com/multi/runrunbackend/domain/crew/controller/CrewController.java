package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.dto.req.CrewCreateReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewStatusChangeReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewUpdateReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewDetailResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewListPageResDto;
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

    /**
     * @param customUser 인증된 사용자 정보
     * @param crewId     크루 ID
     * @description : 크루 삭제 (해체)
     */
    @DeleteMapping("/{crewId}")
    public ResponseEntity<ApiResponse<Void>> deleteCrew(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long crewId
    ) {
        String loginId = customUser.getEmail();
        crewService.deleteCrew(crewId, loginId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("크루 해체 성공", null));
    }

    /**
     * @param cursor     마지막 조회 크루 ID (다음 페이지 조회 시 사용)
     * @param size       페이지 크기 (기본값: 5)
     * @param region     지역 필터 (선택)
     * @param distance   거리 필터 (선택)
     * @param recruiting 모집중 우선 정렬 (선택)
     * @param keyword    크루명 검색 (선택)
     * @description : 크루 목록 조회 (커서 기반 페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CrewListPageResDto>> getCrewList(
            @RequestParam(required = false) Long cursor,

            @RequestParam(defaultValue = "5") int size,

            @RequestParam(required = false) String region,

            @RequestParam(required = false) String distance,

            @RequestParam(required = false) Boolean recruiting,

            @RequestParam(required = false) String keyword
    ) {
        CrewListPageResDto response = crewService.getCrewList(
                cursor, size, region, distance, recruiting, keyword);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("크루 목록 조회 성공", response));
    }

    /**
     * @param crewId 크루 ID
     * @description : 크루 상세 조회
     */
    @GetMapping("/{crewId}")
    public ResponseEntity<ApiResponse<CrewDetailResDto>> getCrewDetail(
            @PathVariable Long crewId
    ) {
        CrewDetailResDto response = crewService.getCrewDetail(crewId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("크루 상세 조회 성공", response));
    }

    /**
     * @param customUser 인증된 사용자 정보
     * @param crewId     크루 ID
     * @param reqDto     모집 상태 변경 요청 DTO
     * @description : 크루 모집 상태 변경
     */
    @PatchMapping("/{crewId}/status")
    public ResponseEntity<ApiResponse<Void>> updateRecruitStatus(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long crewId,
            @Valid @RequestBody CrewStatusChangeReqDto reqDto
    ) {
        String loginId = customUser.getEmail();
        crewService.updateRecruitStatus(loginId, crewId, reqDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("모집 상태 변경 성공", null));
    }
}
