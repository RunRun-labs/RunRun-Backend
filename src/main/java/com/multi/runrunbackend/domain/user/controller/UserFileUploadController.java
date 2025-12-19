package com.multi.runrunbackend.domain.user.controller;

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
 * @description : Please explain the class!!!
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
            throw new IllegalStateException("인증 사용자 정보가 없습니다.");
        }

        User user = userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));


        String fileUrl = fileStorage.upload(
                file,
                domainType,
                user.getId()
        );

        return Map.of("url", fileUrl);
    }
}