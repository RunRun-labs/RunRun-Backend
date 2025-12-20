package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserProfileUploadResDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import com.multi.runrunbackend.domain.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author : kimyongwon
 * @description :
 * 인증된 사용자의 계정 및 프로필 정보를 조회/수정하기 위한 REST 컨트롤러.
 * 사용자 정보 API를 제공한다.
 * 주요 기능:
 * - 내 정보 조회 (/users)
 * - 내 정보 수정 (/users)
 * @filename : UserController
 * @since : 25. 12. 18. 오후 4:22 목요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserProfileController {

    private final UserProfileService userService;
    private final FileStorage fileStorage;
    private final UserRepository userRepository;

    private static final long MAX_PROFILE_IMAGE_SIZE = 1L * 1024 * 1024;

    /**
     * 내 정보 조회
     */
    @GetMapping
    public UserResDto getUser(@AuthenticationPrincipal CustomUser principal) {
        return userService.getUser(principal);
    }

    /**
     * 내 정보 수정
     */
    @PutMapping
    public void updateUser(@RequestBody @Valid UserUpdateReqDto req, @AuthenticationPrincipal CustomUser principal) {
        userService.updateUser(req, principal);
    }

    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/profile-image")
    public ResponseEntity<UserProfileUploadResDto> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUser principal) {

        String url = userService.uploadProfileImage(file, principal);
        return ResponseEntity.ok(new UserProfileUploadResDto(url));
    }
}