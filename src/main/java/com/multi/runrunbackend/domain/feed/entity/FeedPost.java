package com.multi.runrunbackend.domain.feed.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description :  사용자의 러닝 기록을 피드에 공유한 게시물 엔터티 - 피드 목록 조회 - 게시물 수정 / 삭제 - 좋아요 / 댓글의 기준 엔터티
 * @filename : FeedPost
 * @since : 25. 12. 17. 오후 1:28 수요일
 */
@Entity
@Table(name = "feed_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runing_result_id", nullable = false)
    private RunningResult runningResult;

    @Column(length = 500)
    private String content;

    public static FeedPost create(User user, RunningResult runningResult, String content) {
        FeedPost post = new FeedPost();
        post.user = user;
        post.runningResult = runningResult;
        post.content = content;
        return post;
    }

    public void updateContent(String content) {
        this.content = content;
    }

}