package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 *
 * @author : kimyongwon
 * @description :
 * 사용자 프로필 이미지 업로드를 처리하는 컨트롤러.
 * 인증된 사용자의 요청을 받아 MultipartFile을 업로드하며,
 * 파일 저장은 FileStorage 구현체(LocalFileStorage 등)에 위임한다
 * @filename : FileUploadController
 * @since : 25. 12. 19. 오전 11:48 금요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class UserFileUploadController {

    private final FileStorage fileStorage;
    private final UserRepository userRepository;

    /**
     * 파일 업로드 (프로필 이미지 등)
     */
    @PostMapping("/upload")
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("domainType") FileDomainType domainType,
            @AuthenticationPrincipal CustomUser principal
    ) {

        if (principal == null || principal.getLoginId() == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));


        String fileUrl = fileStorage.upload(
                file,
                domainType,
                user.getId()
        );

        return Map.of("url", fileUrl);
    }
}