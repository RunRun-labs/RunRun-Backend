package com.multi.runrunbackend.domain.course.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.LineString;

public final class GeoJsonConverter {

    private GeoJsonConverter() {
    }

    public static Map<String, Object> toGeoJson(LineString lineString) {
        if (lineString == null) {
            return null;
        }

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "LineString");

        List<List<Double>> coordinates = new ArrayList<>();
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            coordinates.add(List.of(
                lineString.getCoordinateN(i).x,
                lineString.getCoordinateN(i).y
            ));
        }

        geoJson.put("coordinates", coordinates);
        return geoJson;
    }
}
