package com.multi.runrunbackend.domain.feed.service;

import com.multi.runrunbackend.common.exception.custom.*;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.req.FeedPostCreateReqDto;
import com.multi.runrunbackend.domain.feed.dto.req.FeedPostUpdateReqDto;
import com.multi.runrunbackend.domain.feed.dto.res.FeedPostResDto;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.feed.repository.FeedCommentRepository;
import com.multi.runrunbackend.domain.feed.repository.FeedLikeRepository;
import com.multi.runrunbackend.domain.feed.repository.FeedPostRepository;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserBlockRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물 관련 비즈니스 로직 처리 서비스
 * @filename : FeedPostService
 * @since : 26. 1. 3. 오후 9:59 토요일
 */

@Service
@RequiredArgsConstructor
@Transactional
public class FeedPostService {

    private final UserRepository userRepository;
    private final RunningResultRepository runningResultRepository;
    private final FeedPostRepository feedPostRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final UserBlockRepository userBlockRepository;
    private final FeedCommentRepository feedCommentRepository;

    /**
     * 러닝 결과 피드 공유
     */
    public FeedPostResDto createFeedPost(
            CustomUser principal,
            FeedPostCreateReqDto req
    ) {
        User user = getUserByPrincipal(principal);

        RunningResult runningResult =
                runningResultRepository
                        .findByIdAndUserId(req.getRunningResultId(), user.getId())
                        .orElseThrow(() ->
                                new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND)
                        );

        if (runningResult.getRunStatus() != RunStatus.COMPLETED) {
            throw new InvalidRequestException(ErrorCode.RUNNING_RESULT_NOT_COMPLETED);
        }

        if (feedPostRepository.existsByRunningResultId(runningResult.getId())) {
            throw new DuplicateException(ErrorCode.FEED_POST_ALREADY_EXISTS);
        }

        FeedPost feedPost = FeedPost.create(
                user,
                runningResult,
                req.getContent()
        );

        FeedPost saved = feedPostRepository.save(feedPost);

        return FeedPostResDto.from(saved, 0L, 0L);
    }

    /**
     * 피드 삭제
     */
    public void deleteFeed(Long feedId, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        FeedPost feedPost = feedPostRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        if (Boolean.TRUE.equals(feedPost.getIsDeleted())) {
            throw new InvalidRequestException(ErrorCode.FEED_ALREADY_DELETED);
        }

        if (!feedPost.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.FEED_FORBIDDEN);
        }

        feedPost.delete();
    }

    /*
     * 피드 수정
     */
    public FeedPostResDto updateFeedPost(
            Long feedId,
            FeedPostUpdateReqDto req,
            CustomUser principal
    ) {
        User user = getUserByPrincipal(principal);

        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        if (!feedPost.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.FEED_FORBIDDEN);
        }

        feedPost.updateContent(req.getContent());

        long likeCount =
                feedLikeRepository.countByFeedPostAndIsDeletedFalse(feedPost);
        long commentCount =
                feedCommentRepository.countByFeedPostAndIsDeletedFalse(feedPost);

        return FeedPostResDto.from(feedPost, likeCount, commentCount);
    }


    /**
     * 피드 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<FeedPostResDto> getFeedList(
            Pageable pageable,
            CustomUser principal
    ) {
        User me = getUserByPrincipal(principal);

        // 1️⃣ 내가 차단한 유저
        Set<Long> excludedUserIds = userBlockRepository
                .findAllByBlockerId(me.getId())
                .stream()
                .map(ub -> ub.getBlockedUser().getId())
                .collect(Collectors.toSet());

        // 2️⃣ 나를 차단한 유저
        userBlockRepository
                .findAllByBlockedUserId(me.getId())
                .stream()
                .map(ub -> ub.getBlocker().getId())
                .forEach(excludedUserIds::add);

        Page<FeedPost> feedPosts;

        if (excludedUserIds.isEmpty()) {
            feedPosts = feedPostRepository.findAllByIsDeletedFalse(pageable);
        } else {
            feedPosts = feedPostRepository
                    .findByIsDeletedFalseAndUserIdNotIn(pageable, excludedUserIds);
        }

        return feedPosts.map(feedPost -> {
            long likeCount =
                    feedLikeRepository.countByFeedPostAndIsDeletedFalse(feedPost);
            long commentCount =
                    feedCommentRepository.countByFeedPostAndIsDeletedFalse(feedPost);
            return FeedPostResDto.from(feedPost, likeCount, commentCount);
        });
    }

    /**
     * 내가 작성한 피드 조회
     */
    @Transactional(readOnly = true)
    public Page<FeedPostResDto> getMyFeedList(
            CustomUser principal,
            Pageable pageable
    ) {
        User user = getUserByPrincipal(principal);

        return feedPostRepository
                .findByUserIdAndIsDeletedFalse(user.getId(), pageable)
                .map(feedPost -> {
                    long likeCount =
                            feedLikeRepository.countByFeedPostAndIsDeletedFalse(feedPost);
                    long commentCount =
                            feedCommentRepository.countByFeedPostAndIsDeletedFalse(feedPost);

                    return FeedPostResDto.from(feedPost, likeCount, commentCount);
                });
    }


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