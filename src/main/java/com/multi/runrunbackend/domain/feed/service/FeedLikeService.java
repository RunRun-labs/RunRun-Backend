package com.multi.runrunbackend.domain.feed.service;

import com.multi.runrunbackend.common.exception.custom.DuplicateException;
import com.multi.runrunbackend.common.exception.custom.InvalidRequestException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.entity.FeedLike;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.feed.repository.FeedLikeRepository;
import com.multi.runrunbackend.domain.feed.repository.FeedPostRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 피드 좋아요/취소 비즈니스 로직
 * @filename : FeedLikeService
 * @since : 26. 1. 5. 오전 10:47 월요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FeedLikeService {

    private final FeedPostRepository feedPostRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final UserRepository userRepository;

    /**
     * 피드 좋아요
     */
    public void likeFeed(Long feedId, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        Optional<FeedLike> existingLike =
                feedLikeRepository.findByFeedPostAndUser(feedPost, user);


        if (existingLike.isPresent()) {
            FeedLike like = existingLike.get();

            if (!like.getIsDeleted()) {
                throw new DuplicateException(ErrorCode.FEED_ALREADY_LIKED);
            }

            like.restore();
            return;
        }

        // 최초 좋아요
        FeedLike newLike = FeedLike.create(feedPost, user);
        feedLikeRepository.save(newLike);
    }

    /**
     * 피드 좋아요 취소
     */
    public void unlikeFeed(Long feedId, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEED_NOT_FOUND));

        FeedLike feedLike = feedLikeRepository
                .findByFeedPostAndUser(feedPost, user)
                .filter(like -> !like.getIsDeleted())
                .orElseThrow(() -> new InvalidRequestException(ErrorCode.FEED_NOT_LIKED));

        feedLike.delete(); // isDeleted = true
    }


    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}