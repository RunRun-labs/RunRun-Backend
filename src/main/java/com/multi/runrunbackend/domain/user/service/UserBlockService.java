package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.friend.repository.FriendRepository;
import com.multi.runrunbackend.domain.user.dto.res.UserBlockResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserBlock;
import com.multi.runrunbackend.domain.user.repository.UserBlockRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author : kimyongwon
 * @description : 차단, 차단 해제, 목록 조회 비즈니스 로직.
 * @filename : UserBlockService
 * @since : 25. 12. 29. 오전 12:12 월요일
 */
@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;


    @Transactional
    public void blockUser(Long targetUserId, CustomUser principal) {
        User blocker = getUserByPrincipal(principal);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        if (blocker.getId().equals(targetUser.getId())) {
            throw new BadRequestException(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        if (userBlockRepository.existsByBlockerAndBlockedUser(blocker, targetUser)) {
            throw new ForbiddenException(ErrorCode.ALREADY_BLOCKED);
        }

        // 차단 시 기존 친구 관계 삭제 추가
        friendRepository.findBetweenUsers(blocker, targetUser)
                .ifPresent(friendRepository::delete);

        UserBlock userBlock = UserBlock.block(blocker, targetUser);
        userBlockRepository.save(userBlock);
    }


    @Transactional
    public void unblockUser(Long targetUserId, CustomUser principal) {
        User blocker = getUserByPrincipal(principal);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        UserBlock userBlock = userBlockRepository.findByBlockerAndBlockedUser(blocker, targetUser)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));

        userBlockRepository.delete(userBlock);
    }


    @Transactional(readOnly = true)
    public List<UserBlockResDto> getBlockedUsers(CustomUser principal) {
        User blocker = getUserByPrincipal(principal);

        return userBlockRepository.findAllByBlockerId(blocker.getId()).stream()
                .map(UserBlockResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void validateUserBlockStatus(Long targetUserId, CustomUser principal) {

        User currentUser = userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // 1. 내가 상대를 차단한 경우
        if (userBlockRepository.existsByBlockerAndBlockedUser(currentUser, targetUser)) {
            throw new ForbiddenException(ErrorCode.USER_BLOCKED);
        }

        // 2. 상대가 나를 차단한 경우 (추가된 요구사항)
        if (userBlockRepository.existsByBlockerAndBlockedUser(targetUser, currentUser)) {
            throw new ForbiddenException(ErrorCode.BLOCKED_BY_USER);
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