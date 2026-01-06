package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.friend.repository.FriendRepository;
import com.multi.runrunbackend.domain.match.dto.res.ProfileRunningHistoryResDto;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.constant.ProfileVisibility;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserSetting;
import com.multi.runrunbackend.domain.user.repository.UserBlockRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import com.multi.runrunbackend.domain.user.repository.UserSettingRepository;
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
    private final UserSettingRepository userSettingRepository;
    private final FriendRepository friendRepository;

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


        validateProfileAccess(me, target);
        
        return runningResultRepository
                .findCompletedByUser(target, pageable)
                .map(ProfileRunningHistoryResDto::from);
    }

    /**
     * 프로필 접근 권한 검증
     */
    private void validateProfileAccess(User me, User target) {

        if (me.getId().equals(target.getId())) {
            return;
        }


        if (userBlockRepository.existsByBlockerAndBlockedUser(me, target)
                || userBlockRepository.existsByBlockerAndBlockedUser(target, me)) {
            throw new ForbiddenException(ErrorCode.USER_BLOCKED);
        }

        UserSetting setting =
                userSettingRepository.findByUserId(target.getId())
                        .orElseGet(() ->
                                userSettingRepository.save(
                                        UserSetting.createDefault(target)
                                )
                        );

        ProfileVisibility visibility = setting.getProfileVisibility();

        switch (visibility) {
            case PUBLIC -> {

            }

            case FRIENDS_ONLY -> {
                boolean isFriend =
                        friendRepository.findBetweenUsers(me, target)
                                .filter(Friend::isAccepted)
                                .isPresent();

                if (!isFriend) {
                    throw new ForbiddenException(ErrorCode.PROFILE_FRIENDS_ONLY);
                }
            }

            case PRIVATE -> {
                throw new ForbiddenException(ErrorCode.PROFILE_PRIVATE);
            }
        }
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}