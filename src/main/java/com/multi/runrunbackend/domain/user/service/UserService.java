package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.common.exception.custom.DuplicateUsernameException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description :
 * 사용자 도메인의 핵심 비즈니스 로직을 담당하는 서비스 클래스.
 * 컨트롤러로부터 전달받은 요청을 도메인 규칙에 맞게 수행한다.
 * 주요 책임:
 * - 로그인 사용자 조회
 * - 사용자 프로필/계정 정보 수정
 * - 사용자 존재 여부 검증 및 예외 처리
 * @filename : UserService
 * @since : 25. 12. 18. 오후 4:23 목요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResDto getUser(CustomUser principal) {
        User user = getUserByPrincipal(principal);
        return UserResDto.from(user);
    }

    public void updateUser(UserUpdateReqDto req, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        // 이메일
        if (!req.getUserEmail().equals(user.getEmail())) {
            validateDuplicateEmail(req.getUserEmail());
            user.updateAccount(req.getUserEmail(), user.getName());
        }

        // 이름
        if (!req.getUserName().equals(user.getName())) {
            user.updateAccount(user.getEmail(), req.getUserName());
        }

        // 키 / 몸무게 (PUT 정책이므로 null 허용 안 됨)
        user.updateProfile(req.getHeightCm(), req.getWeightKg());

        // 프로필 이미지
        if (req.getProfileImageUrl() != null) {
            user.updateProfileImage(req.getProfileImageUrl());
        }
    }

    private User getUserByPrincipal(CustomUser principal) {

        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUsernameException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}