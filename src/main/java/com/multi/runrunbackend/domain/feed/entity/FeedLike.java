package com.multi.runrunbackend.domain.feed.entity;

import com.multi.runrunbackend.common.entitiy.BaseSoftDeleteEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물에 대한 좋아요 엔터티 - 좋아요 추가 / 취소 - 사용자당 게시물 1회만 가능
 * @filename : FeedLike
 * @since : 25. 12. 17. 오후 2:12 수요일
 */
@Entity
@Table(
    name = "feed_like",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"feed_post_id", "user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_post_id", nullable = false)
    private FeedPost feedPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static FeedLike create(FeedPost feedPost, User user) {
        FeedLike like = new FeedLike();
        like.feedPost = feedPost;
        like.user = user;
        return like;
    }
}