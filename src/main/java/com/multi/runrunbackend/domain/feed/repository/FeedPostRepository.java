package com.multi.runrunbackend.domain.feed.repository;

import com.multi.runrunbackend.domain.feed.dto.FeedPostWithCountsDto;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


    Page<FeedPost> findByUserIdAndIsDeletedFalse(
            Long userId,
            Pageable pageable
    );

    Optional<FeedPost> findByIdAndIsDeletedFalse(Long id);

    boolean existsByRunningResultIdAndIsDeletedFalse(Long runningResultId);


    @Query("""
                SELECT new com.multi.runrunbackend.domain.feed.dto.FeedPostWithCountsDto(
                    fp,
                    (SELECT COUNT(fl) FROM FeedLike fl WHERE fl.feedPost = fp AND fl.isDeleted = false),
                    (SELECT COUNT(fc) FROM FeedComment fc WHERE fc.feedPost = fp AND fc.isDeleted = false),
                    (SELECT COUNT(fl) > 0 FROM FeedLike fl WHERE fl.feedPost = fp AND fl.user = :user AND fl.isDeleted = false)
                )
                FROM FeedPost fp
                WHERE fp.isDeleted = false AND fp.user.id NOT IN :excludedUserIds
            """)
    Page<FeedPostWithCountsDto> findAllWithCounts(
            @Param("user") User user,
            @Param("excludedUserIds") Set<Long> excludedUserIds,
            Pageable pageable
    );

    @Query("""
                SELECT new com.multi.runrunbackend.domain.feed.dto.FeedPostWithCountsDto(
                    fp,
                    (SELECT COUNT(fl) FROM FeedLike fl WHERE fl.feedPost = fp AND fl.isDeleted = false),
                    (SELECT COUNT(fc) FROM FeedComment fc WHERE fc.feedPost = fp AND fc.isDeleted = false),
                    (SELECT COUNT(fl) > 0 FROM FeedLike fl WHERE fl.feedPost = fp AND fl.user = :user AND fl.isDeleted = false)
                )
                FROM FeedPost fp
                WHERE fp.isDeleted = false
            """)
    Page<FeedPostWithCountsDto> findAllWithCounts(
            @Param("user") User user,
            Pageable pageable
    );
}