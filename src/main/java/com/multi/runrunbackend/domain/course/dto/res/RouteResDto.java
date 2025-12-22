package com.multi.runrunbackend.domain.course.dto.res;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : RouteResponse
 * @since : 2025. 12. 18. Thursday
 */

@Getter
@Setter
public class RouteResDto {

    private double totalDistance; // meters
    private double totalTime;     // seconds

    // [[lng, lat], [lng, lat], ...]
    private List<double[]> lineString;

    // 카카오 static map url (간이 썸네일)
    private String thumbnailUrl;
}
