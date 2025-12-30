package com.multi.runrunbackend.domain.user.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 개인 설정 수정 요청용 DTO
 * @filename : UserSettingReqDto
 * @since : 25. 12. 28. 오후 10:11 일요일
 */
@Getter
@Setter
@NoArgsConstructor
public class UserSettingReqDto {

    private Boolean notificationEnabled;
    private Boolean nightNotificationEnabled;
}