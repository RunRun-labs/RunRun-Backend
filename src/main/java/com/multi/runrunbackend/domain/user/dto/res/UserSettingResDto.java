package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.constant.ProfileVisibility;
import com.multi.runrunbackend.domain.user.entity.UserSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 조회 응답용 DTO
 * @filename : UserSettingResDto
 * @since : 25. 12. 28. 오후 10:13 일요일
 */
@Getter
@Builder
@AllArgsConstructor
public class UserSettingResDto {

    private boolean notificationEnabled;
    private boolean nightNotificationEnabled;
    private ProfileVisibility profileVisibility;

    public static UserSettingResDto from(UserSetting setting) {
        return UserSettingResDto.builder()
                .notificationEnabled(setting.isNotificationEnabled())
                .nightNotificationEnabled(setting.isNightNotificationEnabled())
                .profileVisibility(setting.getProfileVisibility())
                .build();
    }
}