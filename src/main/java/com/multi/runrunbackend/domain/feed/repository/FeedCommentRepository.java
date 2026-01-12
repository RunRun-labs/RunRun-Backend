package com.multi.runrunbackend.domain.feed.repository;

import com.multi.runrunbackend.domain.feed.entity.FeedComment;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    // ✅ 같은 피드에 댓글을 단 사람들의 userId 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT fc.user.id FROM FeedComment fc " +
           "WHERE fc.feedPost = :feedPost AND fc.isDeleted = false")
    List<Long> findDistinctUserIdsByFeedPost(@Param("feedPost") FeedPost feedPost);

}