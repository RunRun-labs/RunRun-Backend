package com.multi.runrunbackend.domain.user.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 계정 정보 수정 요청 DTO
 * - 이메일 / 이름 변경
 * - 비밀번호 변경은 별도
 * @filename : AccountUpdateReqDto
 * @since : 25. 12. 17. 오후 11:43 수요일
 */
@Getter
@NoArgsConstructor
public class AccountUpdateReqDto {

    /**
     * 이메일
     */
    @Email(message = "이메일 형식을 유지해야 합니다")
    @NotBlank(message = "이메일은 필수 입력 사항입니다")
    private String email;

    /**
     * 이름
     */
    @NotBlank(message = "이름은 필수 입력 사항입니다")
    @Size(max = 4, message = "이름은 최대 4자여야 합니다.")
    private String name;
}