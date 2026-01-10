package com.multi.runrunbackend.domain.crew.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : changwoo
 * @description : 크루 채팅방 공지사항 생성/수정 요청 DTO
 * @filename : CrewChatNoticeReqDto
 * @since : 2026-01-05
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewChatNoticeReqDto {

    @NotBlank(message = "공지사항 내용은 필수입니다.")
    @Size(max = 500, message = "공지사항은 500자 이내로 작성해주세요.")
    private String content;
}
