package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.domain.user.context.UserContext;
import com.multi.runrunbackend.domain.user.dto.req.ProfileImageUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.req.ProfileUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.ProfileResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 인증된 사용자의 프로필을 조회/수정하는 서비스 구현체
 * @filename : ProfileServiceImpl
 * @since : 25. 12. 17. 오후 11:01 수요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserContext userContext; // 현재 로그인 사용자 제공 컴포넌트 (가정)

    @Override
    @Transactional(readOnly = true)
    public ProfileResDto getMyProfile() {
        User user = getCurrentUser();
        return ProfileResDto.from(user);
    }

    @Override
    public void updateProfile(ProfileUpdateReqDto dto) {
        User user = getCurrentUser();
        user.updateProfile(dto.getHeightCm(), dto.getWeightKg());
    }

    @Override
    public void updateProfileImage(ProfileImageUpdateReqDto dto) {
        User user = getCurrentUser();
        user.updateProfileImage(dto.getProfileImageUrl());
    }

    /* =========================
       내부 공통 메서드
       ========================= */

    private User getCurrentUser() {
        Long userId = userContext.getUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다."));
    }
}