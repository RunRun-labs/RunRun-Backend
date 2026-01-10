package com.multi.runrunbackend.domain.feed.dto;

import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FeedPostWithCountsDto {
    private FeedPost feedPost;
    private Long likeCount;
    private Long commentCount;
    private Boolean isLiked;
}

