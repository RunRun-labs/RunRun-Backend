package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.res.ProfileRunningHistoryResDto;
import com.multi.runrunbackend.domain.match.service.ProfileRunningHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author : kimyongwon
 * @description : 마이페이지 - 내 러닝 기록 조회 Controller
 * @filename : ProfileRunningHistoryController
 * @since : 26. 1. 4. 오후 7:26 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/records")
public class ProfileRunningHistoryController {

    private final ProfileRunningHistoryService profileRunningHistoryService;

    /**
     * 마이페이지 - 내 러닝 기록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Slice<ProfileRunningHistoryResDto>>> getMyRunningRecords(
            @AuthenticationPrincipal CustomUser principal,
            @PageableDefault(
                    size = 4,
                    sort = "startedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Slice<ProfileRunningHistoryResDto> result =
                profileRunningHistoryService.getMyRunningRecords(principal, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("내 러닝 기록 조회 성공", result)
        );
    }

    /**
     * 타인 러닝 기록 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Slice<ProfileRunningHistoryResDto>>> getUserRunningRecords(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUser principal,
            @PageableDefault(
                    size = 4,
                    sort = "startedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Slice<ProfileRunningHistoryResDto> result =
                profileRunningHistoryService.getUserRunningRecords(
                        userId,
                        principal,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success("러닝 기록 조회 성공", result)
        );
    }
}