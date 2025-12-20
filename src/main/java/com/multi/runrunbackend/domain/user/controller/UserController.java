package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import com.multi.runrunbackend.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
public class UserController {

    private final UserService userService;
    private final FileStorage fileStorage;
    private final UserRepository userRepository;

    private static final long MAX_PROFILE_IMAGE_SIZE = 1L * 1024 * 1024;

    /**
     * 내 정보 조회
     */
    @GetMapping
    public UserResDto getUser(
            @AuthenticationPrincipal CustomUser principal
    ) {
        return userService.getUser(principal);
    }

    /**
     * 내 정보 수정
     */
    @PutMapping
    public void updateUser(
            @RequestBody @Valid UserUpdateReqDto req,
            @AuthenticationPrincipal CustomUser principal
    ) {
        userService.updateUser(req, principal);
    }


    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/profile-image")
    public Map<String, String> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUser principal
    ) {

        if (file == null || file.isEmpty()) {
            throw new NotFoundException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        User user = userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        String fileUrl = fileStorage.upload(
                file,
                FileDomainType.PROFILE,
                user.getId()
        );

        return Map.of("url", fileUrl);
    }

}