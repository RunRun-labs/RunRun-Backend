package com.multi.runrunbackend.domain.feed.service;

import com.multi.runrunbackend.common.exception.custom.*;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.FeedPostWithCountsDto;
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
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorage fileStorage;

    /**
     * 러닝 결과 피드 공유
     */
    public FeedPostResDto createFeedPost(
            CustomUser principal,
            FeedPostCreateReqDto req,
            MultipartFile imageFile
    ) {
        User user = getUserByPrincipal(principal);

        RunningResult runningResult =
                runningResultRepository
                        .findByIdAndUserIdAndIsDeletedFalse(req.getRunningResultId(), user.getId())
                        .orElseThrow(() ->
                                new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND)
                        );

        if (runningResult.getRunStatus() != RunStatus.COMPLETED) {
            throw new InvalidRequestException(ErrorCode.RUNNING_RESULT_NOT_COMPLETED);
        }

        if (feedPostRepository.existsByRunningResultIdAndIsDeletedFalse(runningResult.getId())) {
            throw new DuplicateException(ErrorCode.FEED_POST_ALREADY_EXISTS);
        }

        // 이미지 처리: 업로드된 이미지가 있으면 업로드, 없으면 코스 썸네일 사용
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageKey = fileStorage.upload(imageFile, FileDomainType.FEED_IMAGE, user.getId());
            imageUrl = fileStorage.toHttpsUrl(imageKey);
        } else {
            // 코스 썸네일 URL 사용
            if (runningResult.getCourse() != null && runningResult.getCourse().getThumbnailUrl() != null) {
                imageUrl = fileStorage.toHttpsUrl(runningResult.getCourse().getThumbnailUrl());
            }
        }

        FeedPost feedPost = FeedPost.create(
                user,
                runningResult,
                req.getContent(),
                imageUrl
        );

        FeedPost saved = feedPostRepository.save(feedPost);

        return FeedPostResDto.from(saved, 0L, 0L, false);
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

        // 관리자가 아니고 작성자가 아닌 경우 삭제 불가
        boolean isAdmin = principal != null && principal.isAdmin();
        if (!isAdmin && !feedPost.getUser().getId().equals(user.getId())) {
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
        boolean isLiked = feedLikeRepository.existsByFeedPostAndUserAndIsDeletedFalse(feedPost, user);

        return FeedPostResDto.from(feedPost, likeCount, commentCount, isLiked);
    }

    /**
     * 피드 상세 조회
     */
    @Transactional(readOnly = true)
    public FeedPostResDto getFeedPost(Long feedId, CustomUser principal) {
        User me = getUserByPrincipal(principal);

        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        long likeCount = feedLikeRepository.countByFeedPostAndIsDeletedFalse(feedPost);
        long commentCount = feedCommentRepository.countByFeedPostAndIsDeletedFalse(feedPost);
        boolean isLiked = feedLikeRepository.existsByFeedPostAndUserAndIsDeletedFalse(feedPost, me);

        return FeedPostResDto.from(feedPost, likeCount, commentCount, isLiked);
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

        Set<Long> excludedUserIds = userBlockRepository
                .findAllByBlockerId(me.getId())
                .stream()
                .map(ub -> ub.getBlockedUser().getId())
                .collect(Collectors.toSet());

        userBlockRepository
                .findAllByBlockedUserId(me.getId())
                .stream()
                .map(ub -> ub.getBlocker().getId())
                .forEach(excludedUserIds::add);

        Page<FeedPostWithCountsDto> feedPosts;

        if (excludedUserIds.isEmpty()) {
            feedPosts = feedPostRepository.findAllWithCounts(me, pageable);
        } else {
            feedPosts = feedPostRepository.findAllWithCounts(me, excludedUserIds, pageable);
        }

        return feedPosts.map(dto -> FeedPostResDto.from(
                dto.getFeedPost(),
                dto.getLikeCount(),
                dto.getCommentCount(),
                dto.getIsLiked()
        ));
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
                    boolean isLiked = feedLikeRepository.existsByFeedPostAndUserAndIsDeletedFalse(feedPost, user);
                    return FeedPostResDto.from(feedPost, likeCount, commentCount, isLiked);
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