package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.domain.user.dto.req.ProfileImageUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.req.ProfileUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.ProfileResDto;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 프로필 관련 비즈니스 로직을 담당한다.
 * - 프로필 조회
 * - 프로필 정보 수정 (키/몸무게)
 * - 프로필 이미지 변경
 * @filename : ProfileService
 * @since : 25. 12. 17. 오후 11:00 수요일
 */
public interface ProfileService {

    ProfileResDto getMyProfile();

    void updateProfile(ProfileUpdateReqDto dto);

    void updateProfileImage(ProfileImageUpdateReqDto dto);
}