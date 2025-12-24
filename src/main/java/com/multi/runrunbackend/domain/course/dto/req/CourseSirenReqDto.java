package com.multi.runrunbackend.domain.course.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSirenReqDto {

    @NotBlank(message = "신고 사유를 반드시 입력해주세요.")
    @Size(
        min = 10,
        max = 500,
        message = "신고 이유는 10자 이상 500자 이하로 작성해주세요."
    )
    private String description;
}