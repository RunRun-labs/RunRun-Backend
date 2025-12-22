package com.multi.runrunbackend.domain.course.dto.req;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CoursorPage
 * @since : 2025. 12. 19. Friday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T> {

    private List<T> items;
    private String nextCursor;
    private boolean hasNext;
}