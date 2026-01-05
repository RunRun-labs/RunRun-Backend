package com.multi.runrunbackend.domain.feed.repository;

import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물 Repository
 * @filename : FeedPostRepository
 * @since : 26. 1. 3. 오후 9:53 토요일
 */
public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    Page<FeedPost> findAllByIsDeletedFalse(Pageable pageable);

    Page<FeedPost> findByUserIdAndIsDeletedFalse(
            Long userId,
            Pageable pageable
    );

    Optional<FeedPost> findByIdAndIsDeletedFalse(Long id);

    boolean existsByRunningResultId(Long runningResultId);

    Page<FeedPost> findByIsDeletedFalseAndUserIdNotIn(Pageable pageable, Set<Long> excludedUserIds);
}