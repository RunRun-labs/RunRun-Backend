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
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.notification.service.NotificationService;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author : kimyongwon
 * @description : 피드 댓글 등록/삭제/조회 서비스
 * @filename : FeedCommentService
 * @since : 26. 1. 5. 오후 1:03 월요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedCommentService {

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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

        // ✅ 댓글 작성 시 알림 발송 - 피드 게시자 + 댓글 단 사람들 (자신 제외)
        try {
            User feedPostAuthor = feedPost.getUser();
            Long commenterId = user.getId();

            // 같은 피드에 댓글을 단 사람들의 userId 목록 조회 (중복 제거)
            Set<Long> commenterIds = feedCommentRepository
                    .findDistinctUserIdsByFeedPost(feedPost)
                    .stream()
                    .filter(id -> !id.equals(commenterId)) // 자신 제외
                    .collect(Collectors.toSet());

            // 피드 게시자에게 알림 발송 (게시자가 댓글 작성자가 아닌 경우)
            if (!feedPostAuthor.getId().equals(commenterId)) {
                try {
                    notificationService.create(
                            feedPostAuthor,
                            "댓글 알림",
                            user.getName() + "님이 내 게시물에 댓글을 남겼습니다.",
                            NotificationType.FEED,
                            RelatedType.FEED_RECORD,
                            feedPost.getId()
                    );
                    log.debug("피드 댓글 알림 발송 완료 (게시자) - feedId: {}, receiverId: {}, commenterId: {}",
                            feedPost.getId(), feedPostAuthor.getId(), commenterId);
                } catch (Exception e) {
                    log.error("피드 댓글 알림 생성 실패 (게시자) - feedId: {}, receiverId: {}, commenterId: {}",
                            feedPost.getId(), feedPostAuthor.getId(), commenterId, e);
                }
            }

            // 댓글 단 사람들에게 알림 발송 (피드 게시자 제외, 자신 제외)
            int commenterNotificationCount = 0;
            for (Long receiverId : commenterIds) {
                // 피드 게시자는 이미 처리했으므로 제외
                if (receiverId.equals(feedPostAuthor.getId())) {
                    continue;
                }

                try {
                    User receiver = userRepository.findById(receiverId)
                            .orElse(null);
                    if (receiver == null) {
                        continue;
                    }

                    notificationService.create(
                            receiver,
                            "댓글 알림",
                            user.getName() + "님이 내가 댓글을 단 게시물에 댓글을 남겼습니다.",
                            NotificationType.FEED,
                            RelatedType.FEED_RECORD,
                            feedPost.getId()
                    );
                    commenterNotificationCount++;
                    log.debug("피드 댓글 알림 발송 완료 (댓글 작성자) - feedId: {}, receiverId: {}, commenterId: {}",
                            feedPost.getId(), receiverId, commenterId);
                } catch (Exception e) {
                    log.error("피드 댓글 알림 생성 실패 (댓글 작성자) - feedId: {}, receiverId: {}, commenterId: {}",
                            feedPost.getId(), receiverId, commenterId, e);
                    // 개별 실패는 전체 알림 발송을 중단하지 않음
                }
            }

            int totalNotificationCount = commenterNotificationCount +
                    (feedPostAuthor.getId().equals(commenterId) ? 0 : 1);
            log.info("피드 댓글 알림 발송 완료 - feedId: {}, commenterId: {}, 알림 수신자 수: {} (게시자: {}, 댓글 작성자: {})",
                    feedPost.getId(), commenterId, totalNotificationCount,
                    feedPostAuthor.getId().equals(commenterId) ? 0 : 1,
                    commenterNotificationCount);
        } catch (Exception e) {
            log.error("피드 댓글 알림 발송 중 오류 발생 - feedId: {}, commenterId: {}",
                    feedPost.getId(), user.getId(), e);
            // 알림 발송 실패해도 댓글 등록은 유지
        }
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