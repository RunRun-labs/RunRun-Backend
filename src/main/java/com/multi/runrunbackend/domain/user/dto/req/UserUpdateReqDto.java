package com.multi.runrunbackend.domain.user.dto.req;

import jakarta.validation.constraints.*;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description 사용자 프로필 및 계정 정보 수정을 위한 요청 DTO.
 * 마이페이지 수정 화면에서 전달되는 입력 값을 담아 서버로 전달
 * @filename : UserUpdateReqDto
 * @since : 25. 12. 18. 오후 4:27 목요일
 */

@Getter
public class UserUpdateReqDto {

    @Email(message = "이메일 형식을 유지해야 합니다")
    @NotBlank(message = "이메일은 필수 입력 사항입니다")
    private String userEmail;

    @NotBlank(message = "이름은 필수 입력 사항입니다")
    @Size(max = 4, message = "이름은 최대 4자여야 합니다.")
    private String userName;

    @Min(value = 50, message = "키는 50cm 이상이어야 합니다.")
    @Max(value = 300, message = "키는 300cm 이하이어야 합니다.")
    private Integer heightCm;

    @Min(value = 10, message = "몸무게는 10kg 이상이어야 합니다.")
    @Max(value = 500, message = "몸무게는 500kg 이하이어야 합니다.")
    private Integer weightKg;

    private String profileImageUrl;
}
