package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.entity.CrewChatNotice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : changwoo
 * @description : 크루 채팅방 공지사항 응답 DTO
 * @filename : CrewChatNoticeResDto
 * @since : 2026-01-05
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewChatNoticeResDto {

    private Long id;
    private Long roomId;
    private String content;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static CrewChatNoticeResDto fromEntity(CrewChatNotice notice) {
        return CrewChatNoticeResDto.builder()
                .id(notice.getId())
                .roomId(notice.getRoom().getId())
                .content(notice.getContent())
                .createdBy(notice.getCreatedBy().getId())
                .createdByName(notice.getCreatedBy().getName())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
