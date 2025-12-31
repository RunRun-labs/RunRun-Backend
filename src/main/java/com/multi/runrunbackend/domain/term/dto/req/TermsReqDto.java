package com.multi.runrunbackend.domain.term.dto.req;

import com.multi.runrunbackend.domain.term.constant.TermsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author : kimyongwon
 * @description : 약관 생성 요청 객체
 * @filename : TermsReqDto
 * @since : 25. 12. 29. 오전 10:03 월요일
 */
@Getter
@Setter
@NoArgsConstructor
public class TermsReqDto {

    @NotNull(message = "약관 타입은 필수입니다.")
    private TermsType type;

    @NotBlank(message = "버전은 필수입니다.")
    private String version;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "필수 동의 여부는 필수입니다.")
    private Boolean required;
}