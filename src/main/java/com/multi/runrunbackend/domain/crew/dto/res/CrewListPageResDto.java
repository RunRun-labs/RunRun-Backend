package com.multi.runrunbackend.domain.crew.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 목록 페이징 응답 DTO (커서 기반)
 * @filename : CrewListPageResDto
 * @since : 25. 12. 18. 목요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "크루 목록 페이징 응답")
public class CrewListPageResDto {

    @Schema(description = "크루 목록")
    private List<CrewListResDto> crews;

    @Schema(description = "다음 커서 (다음 페이지 조회 시 사용)")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부")
    private Boolean hasMore;

    /**
     * @param crews    크루 목록
     * @param pageSize 페이지 크기
     * @description : toDtoPage - 페이징 응답 생성 (Entity 리스트 → 페이징 DTO 변환)
     */
    public static CrewListPageResDto toDtoPage(List<CrewListResDto> crews, int pageSize) {
        boolean hasMore = crews.size() == pageSize;
        Long nextCursor = hasMore ? crews.get(crews.size() - 1).getCrewId() : null;

        return CrewListPageResDto.builder()
                .crews(crews)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
