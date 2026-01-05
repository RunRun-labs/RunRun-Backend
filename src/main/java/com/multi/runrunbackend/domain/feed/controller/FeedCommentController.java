package com.multi.runrunbackend.domain.feed.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.req.FeedCommentCreateReqDto;
import com.multi.runrunbackend.domain.feed.service.FeedCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author : kimyongwon
 * @description : 피드 댓글 등록/삭제 컨트롤러
 * @filename : FeedCommentController
 * @since : 26. 1. 5. 오후 12:59 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feed/{feedId}/comments")
public class FeedCommentController {

    private final FeedCommentService feedCommentService;

    /**
     * 댓글 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createComment(
            @PathVariable Long feedId,
            @RequestBody @Valid FeedCommentCreateReqDto req,
            @AuthenticationPrincipal CustomUser principal
    ) {
        feedCommentService.createComment(feedId, req, principal);
        return ResponseEntity.ok(
                ApiResponse.successNoData("댓글 등록 성공")
        );
    }

    /**
     * 댓글 삭제 (soft)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long feedId, // URL 일관성
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        feedCommentService.deleteComment(commentId, principal);
        return ResponseEntity.ok(
                ApiResponse.successNoData("댓글 삭제 성공")
        );
    }
}