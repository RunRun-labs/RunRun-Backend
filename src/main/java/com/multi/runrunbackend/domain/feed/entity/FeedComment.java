package com.multi.runrunbackend.domain.feed.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물에 작성된 댓글 엔터티
 * @filename : FeedComment
 * @since : 25. 12. 17. 오후 2:09 수요일
 */
@Entity
@Table(name = "feed_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_post_id", nullable = false)
    private FeedPost feedPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 300)
    private String content;

    public static FeedComment create(FeedPost feedPost, User user, String content) {
        FeedComment comment = new FeedComment();
        comment.feedPost = feedPost;
        comment.user = user;
        comment.content = content;
        return comment;
    }

}