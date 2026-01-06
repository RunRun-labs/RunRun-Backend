package com.multi.runrunbackend.domain.point.dto.req;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 커서 기반 페이징 응답
 * @filename : CursorPage
 * @since : 2026. 01. 05. 월요일
 */
@Getter
@Builder
public class CursorPage<T> {
    private List<T> items;
    private boolean hasNext;
    private Long nextCursor;
}
