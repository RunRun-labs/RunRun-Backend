package com.multi.runrunbackend.domain.feed.repository;

import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물 Repository
 * @filename : FeedPostRepository
 * @since : 26. 1. 3. 오후 9:53 토요일
 */
public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    boolean existsByRunningResultId(Long runningResultId);
}