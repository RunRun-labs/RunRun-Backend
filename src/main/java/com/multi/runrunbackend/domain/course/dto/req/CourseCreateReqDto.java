package com.multi.runrunbackend.domain.course.dto.req;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseCreateReq
 * @since : 2025. 12. 18. Thursday
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreateReqDto {

    private String title;
    private String description;
    @JsonDeserialize(using = LineStringDeserializer.class)
    private LineString path;
    private Integer distanceM;
    private Double startLat;
    private Double startLng;
    private String address;
    private CourseRegisterType courseRegisterType;

}
