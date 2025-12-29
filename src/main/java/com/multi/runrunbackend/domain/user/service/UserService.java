package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.common.exception.custom.DuplicateException;
import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 도메인의 핵심 비즈니스 로직을 담당하는 서비스 클래스. 컨트롤러로부터 전달받은 요청을 도메인 규칙에 맞게 수행한다. 주요 책임: - 로그인
 * 사용자 조회 - 사용자 프로필/계정 정보 수정 - 사용자 존재 여부 검증 및 예외 처리
 * @filename : UserService
 * @since : 25. 12. 18. 오후 4:23 목요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private static final long MAX_PROFILE_IMAGE_SIZE = 1L * 1024 * 1024;

    @Transactional(readOnly = true)
    public UserResDto getUser(CustomUser principal) {
        User user = getUserByPrincipal(principal);
        return UserResDto.from(user);
    }

    public void updateUser(UserUpdateReqDto req, MultipartFile file, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        updateEmailIfChanged(req, user);
        updateNameIfChanged(req, user);
        updateBodyInfo(req, user);
        updateProfileImage(req, file, user);
    }

    public void deleteUser(CustomUser principal) {
        User user = getUserByPrincipal(principal);
        if (user.isDeleted()) {
            throw new DuplicateException(ErrorCode.USER_ALREADY_DELETED);
        }
        user.deleteAccount();
    }


    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }
        String loginId = principal.getLoginId();

        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }


    private void updateEmailIfChanged(UserUpdateReqDto req, User user) {
        if (!req.getUserEmail().equals(user.getEmail())) {
            validateDuplicateEmail(req.getUserEmail());
            user.updateAccount(req.getUserEmail(), user.getName());
        }
    }

    private void updateNameIfChanged(UserUpdateReqDto req, User user) {
        if (!req.getUserName().equals(user.getName())) {
            user.updateAccount(user.getEmail(), req.getUserName());
        }
    }

    private void updateBodyInfo(UserUpdateReqDto req, User user) {
        user.updateProfile(req.getHeightCm(), req.getWeightKg());
    }

    private void updateProfileImage(UserUpdateReqDto req, MultipartFile file, User user) {
        String finalUrl = req.getProfileImageUrl();

        if (file != null && !file.isEmpty()) {
            validateProfileImage(file);
            finalUrl = fileStorage.upload(file, FileDomainType.PROFILE, user.getId());
        }

        user.updateProfileImage(finalUrl);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(ErrorCode.FILE_EMPTY);
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new FileUploadException(ErrorCode.FILE_NOT_IMAGE);
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new FileUploadException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }
}