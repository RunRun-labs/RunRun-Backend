package com.multi.runrunbackend.domain.user.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 프로필 이미지 업로드 응답 DTO
 * @filename : ProfileImageUploadResDto
 * @since : 25. 12. 20. 오후 12:38 토요일
 */
@Getter
@AllArgsConstructor
public class UserProfileUploadResDto {
    private final String url;
}