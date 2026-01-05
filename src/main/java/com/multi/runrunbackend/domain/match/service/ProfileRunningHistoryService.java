package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.res.ProfileRunningHistoryResDto;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserBlockRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 마이페이지 - 러닝 기록 조회 Service
 * @filename : ProfileRunningHistoryService
 * @since : 26. 1. 4. 오후 7:29 일요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileRunningHistoryService {

    private final RunningResultRepository runningResultRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    /**
     * 내 러닝 기록 조회
     */
    public Slice<ProfileRunningHistoryResDto> getMyRunningRecords(
            CustomUser principal,
            Pageable pageable
    ) {
        User me = getUserByPrincipal(principal);

        return runningResultRepository
                .findCompletedByUser(me, pageable)
                .map(ProfileRunningHistoryResDto::from);
    }

    /**
     * 타 사용자 러닝 기록 조회
     */
    public Slice<ProfileRunningHistoryResDto> getUserRunningRecords(
            Long userId,
            CustomUser principal,
            Pageable pageable
    ) {
        User me = getUserByPrincipal(principal);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));


        if (userBlockRepository.existsByBlockerAndBlockedUser(me, target)
                || userBlockRepository.existsByBlockerAndBlockedUser(target, me)) {
            throw new ForbiddenException(ErrorCode.USER_BLOCKED);
        }

        return runningResultRepository
                .findCompletedByUser(target, pageable)
                .map(ProfileRunningHistoryResDto::from);
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}