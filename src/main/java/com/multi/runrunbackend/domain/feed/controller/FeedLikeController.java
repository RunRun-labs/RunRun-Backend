package com.multi.runrunbackend.domain.feed.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.service.FeedLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author : kimyongwon
 * @description : 피드 좋아요/좋아요 취소 컨트롤러
 * @filename : FeedLikeController
 * @since : 26. 1. 5. 오전 10:46 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feed")
public class FeedLikeController {

    private final FeedLikeService feedLikeService;

    /**
     * 피드 좋아요
     */
    @PostMapping("/{feedId}/like")
    public ResponseEntity<ApiResponse<Void>> likeFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        feedLikeService.likeFeed(feedId, principal);

        return ResponseEntity.ok(
                ApiResponse.successNoData("피드 좋아요 성공")
        );
    }

    /**
     * 피드 좋아요 취소
     */
    @DeleteMapping("/{feedId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        feedLikeService.unlikeFeed(feedId, principal);

        return ResponseEntity.ok(
                ApiResponse.successNoData("피드 좋아요 취소 성공")
        );
    }
}