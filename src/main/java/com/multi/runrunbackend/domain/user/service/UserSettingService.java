package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.dto.req.UserSettingReqDto;
import com.multi.runrunbackend.domain.user.dto.res.UserSettingResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserSetting;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import com.multi.runrunbackend.domain.user.repository.UserSettingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * @author : kimyongwon
 * @description : 설정 조회 및 수정 비즈니스 로직. (없으면 기본값 생성 로직 포함)
 * @filename : UserSettingService
 * @since : 25. 12. 28. 오후 10:21 일요일
 */
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;


    @Transactional
    public UserSettingResDto getUserSetting(CustomUser principal) {
        User user = getUserByPrincipal(principal);

        UserSetting setting = userSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // 설정이 없으면 기본값 생성 및 저장
                    UserSetting newSetting = UserSetting.createDefault(user);
                    return userSettingRepository.save(newSetting);
                });

        return UserSettingResDto.from(setting);
    }

    @Transactional
    public void updateUserSetting(UserSettingReqDto req, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        UserSetting setting = userSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSetting newSetting = UserSetting.createDefault(user);
                    return userSettingRepository.save(newSetting);
                });

        boolean notiEnabled = req.getNotificationEnabled() != null
                ? req.getNotificationEnabled()
                : setting.isNotificationEnabled();

        boolean nightEnabled = req.getNightNotificationEnabled() != null
                ? req.getNightNotificationEnabled()
                : setting.isNightNotificationEnabled();

        setting.updateNotification(notiEnabled, nightEnabled);
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}