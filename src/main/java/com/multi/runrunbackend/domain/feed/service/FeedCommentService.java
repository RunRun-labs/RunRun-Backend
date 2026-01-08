package com.multi.runrunbackend.domain.feed.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.req.FeedCommentCreateReqDto;
import com.multi.runrunbackend.domain.feed.dto.res.FeedCommentResDto;
import com.multi.runrunbackend.domain.feed.entity.FeedComment;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.feed.repository.FeedCommentRepository;
import com.multi.runrunbackend.domain.feed.repository.FeedPostRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 피드 댓글 등록/삭제/조회 서비스
 * @filename : FeedCommentService
 * @since : 26. 1. 5. 오후 1:03 월요일
 */
@Service
@RequiredArgsConstructor
public class FeedCommentService {

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 등록
     */
    @Transactional
    public void createComment(
            Long feedId,
            FeedCommentCreateReqDto req,
            CustomUser principal
    ) {
        User user = getUserByPrincipal(principal);

        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        FeedComment comment =
                FeedComment.create(feedPost, user, req.getContent());

        feedCommentRepository.save(comment);
    }

    /**
     * 댓글 삭제 (soft delete)
     */
    @Transactional
    public void deleteComment(
            Long feedId,
            Long commentId,
            CustomUser principal
    ) {
        User user = getUserByPrincipal(principal);

        FeedComment comment = feedCommentRepository
                .findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.FEED_COMMENT_NOT_FOUND)
                );

        // 댓글이 해당 피드에 속하는지 검증
        if (!comment.getFeedPost().getId().equals(feedId)) {
            throw new NotFoundException(ErrorCode.FEED_COMMENT_NOT_FOUND);
        }

        // 관리자가 아니고 작성자가 아닌 경우 삭제 불가
        boolean isAdmin = principal.isAdmin();
        if (!isAdmin && !comment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.FEED_COMMENT_FORBIDDEN);
        }

        comment.delete();
    }

    /**
     * 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<FeedCommentResDto> getComments(
            Long feedId,
            Pageable pageable
    ) {
        FeedPost feedPost = feedPostRepository
                .findByIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        return feedCommentRepository
                .findByFeedPostAndIsDeletedFalse(feedPost, pageable)
                .map(FeedCommentResDto::from);
    }

    /* 공통 */
    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.USER_NOT_FOUND)
                );
    }
}