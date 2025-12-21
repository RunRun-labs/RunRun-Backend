package com.multi.runrunbackend.domain.course.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TmapPedestrianResponse
 * @since : 2025. 12. 18. Thursday
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmapPedestrianResDto {

    private List<Feature> features;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {

        private Geometry geometry;
        private Map<String, Object> properties;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {

        private String type;
        private JsonNode coordinates;
    }
}
