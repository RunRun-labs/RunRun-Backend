package com.multi.runrunbackend.domain.course.dto.req;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseUpdateReqDto
 * @since : 2025. 12. 20. Saturday
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseUpdateReqDto {

    @NotBlank(message = "코스 제목은 필수입니다")
    @Size(max = 50, message = "코스 제목은 50자 이내여야 합니다")
    private String title;

    @Size(max = 500, message = "코스 설명은 500자 이내여야 합니다")
    private String description;

    @NotNull(message = "코스 경로는 필수입니다")
    private String path;

    @NotNull(message = "코스 거리는 필수입니다")
    @Min(value = 100, message = "코스 거리는 최소 100m 이상이어야 합니다")
    @Max(value = 100_000, message = "코스 거리는 최대 100km를 초과할 수 없습니다")
    private Integer distanceM;

    @NotNull(message = "시작 위도는 필수입니다")
    @DecimalMin(value = "-90.0", inclusive = true)
    @DecimalMax(value = "90.0", inclusive = true)
    private Double startLat;

    @NotNull(message = "시작 경도는 필수입니다")
    @DecimalMin(value = "-180.0", inclusive = true)
    @DecimalMax(value = "180.0", inclusive = true)
    private Double startLng;

    @NotBlank(message = "주소는 필수입니다")
    @Size(max = 100)
    private String address;

    @NotNull(message = "코스 등록 타입은 필수입니다")
    private CourseRegisterType courseRegisterType;

}
