package com.multi.runrunbackend.domain.feed.repository;

import com.multi.runrunbackend.domain.feed.entity.FeedComment;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 피드 댓글 등록/삭제 Repository
 * @filename : FeedCommentRepository
 * @since : 26. 1. 5. 오후 1:01 월요일
 */
public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    Optional<FeedComment> findByIdAndIsDeletedFalse(Long id);


    long countByFeedPostAndIsDeletedFalse(FeedPost feedPost);

    Page<FeedComment> findByFeedPostAndIsDeletedFalse(
            FeedPost feedPost,
            Pageable pageable
    );

}