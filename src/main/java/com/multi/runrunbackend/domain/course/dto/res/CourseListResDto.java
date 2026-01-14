package com.multi.runrunbackend.domain.course.dto.res;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseListResDto
 * @since : 2025. 12. 18. Thursday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseListResDto {

    private Long id;
    private String title;

    private Integer distanceM;
    private String address;


    private String thumbnailUrl;

    private Long likeCount;
    private Long favoriteCount;

    private CourseRegisterType registerType;

    private OffsetDateTime createdAt;

    private Double distM;

    private Boolean isLiked;
    private Boolean isFavorited;

    public void resolveThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

}
