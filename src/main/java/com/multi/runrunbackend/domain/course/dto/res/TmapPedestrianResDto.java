package com.multi.runrunbackend.domain.course.dto.res;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TmapPedestrianResponse
 * @since : 2025. 12. 18. Thursday
 */
@Getter
@Setter
public class TmapPedestrianResDto {

    private List<Feature> features;

    @Getter
    @Setter
    public static class Feature {

        private Geometry geometry;
        private Map<String, Object> properties;
    }

    @Getter
    @Setter
    public static class Geometry {

        private String type;
        private JsonNode coordinates;
    }
}
