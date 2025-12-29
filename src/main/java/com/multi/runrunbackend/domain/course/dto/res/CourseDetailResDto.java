package com.multi.runrunbackend.domain.course.dto.res;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.util.GeoJsonConverter;
import com.multi.runrunbackend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseDetailResDto
 * @since : 2025. 12. 19. Friday
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseDetailResDto {

    private String title;
    private String description;
    private Map<String, Object> path;
    private Integer distanceM;
    private Double startLat;
    private Double startLng;
    private String imageUrl;
    private CourseRegisterType registerType;
    private String address;
    private long likeCount;
    private long favoriteCount;
    private String userName;
    private LocalDateTime createdAt;
    private Boolean isOwner;
    private Boolean isLiked;
    private Boolean isFavorited;

    public static CourseDetailResDto fromEntity(Course course, User user, Boolean isLiked,
        Boolean isFavorited) {
        if (course == null) {
            return null;
        }
        boolean isOwner = user.getId() != null &&
            course.getUser().getId().equals(user.getId());

        return CourseDetailResDto.builder()
            .title(course.getTitle())
            .description(course.getDescription())
            .path(GeoJsonConverter.toGeoJson(course.getPath()))
            .distanceM(course.getDistanceM())
            .startLat(course.getStartLat())
            .startLng(course.getStartLng())
            .imageUrl(course.getImageUrl())
            .registerType(course.getRegisterType())
            .address(course.getAddress())
            .likeCount(course.getLikeCount())
            .favoriteCount(course.getFavoriteCount())
            .isOwner(isOwner)
            .userName(user.getName())
            .createdAt(course.getCreatedAt())
            .isLiked(isLiked)
            .isFavorited(isFavorited)
            .build();
    }
}
