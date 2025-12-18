package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.domain.user.dto.req.ProfileImageUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.req.ProfileUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.ProfileResDto;
import com.multi.runrunbackend.domain.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 프로필 조회 및 수정 API를 제공한다.
 * @filename : ProfileController
 * @since : 25. 12. 17. 오후 11:25 수요일
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResDto> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    /**
     * 프로필 정보 수정 (키/몸무게)
     */
    @PutMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @RequestBody ProfileUpdateReqDto dto
    ) {
        profileService.updateProfile(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 프로필 이미지 변경
     */
    @PatchMapping("/me/image")
    public ResponseEntity<Void> updateProfileImage(
            @RequestBody ProfileImageUpdateReqDto dto
    ) {
        profileService.updateProfileImage(dto);
        return ResponseEntity.ok().build();
    }
}