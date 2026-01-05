package com.multi.runrunbackend.domain.feed.service;

import com.multi.runrunbackend.common.exception.custom.DuplicateException;
import com.multi.runrunbackend.common.exception.custom.InvalidRequestException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.req.FeedPostCreateReqDto;
import com.multi.runrunbackend.domain.feed.dto.res.FeedPostResDto;
import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.feed.repository.FeedPostRepository;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물 관련 비즈니스 로직 처리 서비스
 * @filename : FeedPostService
 * @since : 26. 1. 3. 오후 9:59 토요일
 */

@Service
@RequiredArgsConstructor
@Transactional
public class FeedPostService {

    private final UserRepository userRepository;
    private final RunningResultRepository runningResultRepository;
    private final FeedPostRepository feedPostRepository;

    /**
     * 러닝 결과 피드 공유
     */
    public FeedPostResDto createFeedPost(
            CustomUser principal,
            FeedPostCreateReqDto req
    ) {

        User user = getUserByPrincipal(principal);

        RunningResult runningResult = runningResultRepository
                .findByIdAndUserId(req.getRunningResultId(), user.getId())
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND)
                );


        if (runningResult.getRunStatus() != RunStatus.COMPLETED) {
            throw new InvalidRequestException(ErrorCode.RUNNING_RESULT_NOT_COMPLETED);
        }

        if (feedPostRepository.existsByRunningResultId(runningResult.getId())) {
            throw new DuplicateException(ErrorCode.FEED_POST_ALREADY_EXISTS);
        }

        FeedPost feedPost = FeedPost.create(
                user,
                runningResult,
                req.getContent()
        );

        FeedPost saved = feedPostRepository.save(feedPost);

        return FeedPostResDto.from(saved);
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.USER_NOT_FOUND)
                );
    }
}