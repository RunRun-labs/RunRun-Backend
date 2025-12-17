package com.multi.runrunbackend.domain.user.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 프로필 이미지 변경 요청 DTO
 * - 업로드 된 이미지의 URL만 전달받아 교체
 * @filename : ProfileImageUpdateReqDto
 * @since : 25. 12. 17. 오후 10:56 수요일
 */
@Getter
@NoArgsConstructor
public class ProfileImageUpdateReqDto {

    @NotBlank(message = "프로필 이미지 URL은 필수입니다.")
    private String profileImageUrl;
}