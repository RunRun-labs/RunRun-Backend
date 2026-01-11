package com.multi.runrunbackend.domain.feed.repository;

import com.multi.runrunbackend.domain.feed.entity.FeedLike;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 피드 좋아요 관련 Repository
 * @filename : FeedLikeRepository
 * @since : 26. 1. 5. 오전 10:47 월요일
 */
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByFeedPostAndUser(FeedPost feedPost, User user);

    boolean existsByFeedPostAndUserAndIsDeletedFalse(FeedPost feedPost, User user);

    long countByFeedPostAndIsDeletedFalse(FeedPost feedPost);
}