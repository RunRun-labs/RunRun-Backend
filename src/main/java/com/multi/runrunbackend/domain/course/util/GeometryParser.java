package com.multi.runrunbackend.domain.course.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeometryParser {

    private static final GeometryFactory GEOMETRY_FACTORY =
        new GeometryFactory(new PrecisionModel(), 4326);

    private final ObjectMapper objectMapper;

    public LineString parseLineString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
        }

        raw = raw.trim();

        if (raw.startsWith("{")) {
            return parseGeoJson(raw);
        }

        if (raw.toUpperCase().startsWith("LINESTRING")) {
            return parseWkt(raw);
        }

        throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
    }

    private LineString parseGeoJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (!"LineString".equalsIgnoreCase(root.path("type").asText())) {
                throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
            }

            JsonNode coordsNode = root.path("coordinates");
            if (!coordsNode.isArray() || coordsNode.size() < 2) {
                throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
            }

            List<Coordinate> coords = new ArrayList<>();
            for (JsonNode n : coordsNode) {
                if (n.size() < 2) {
                    continue;
                }
                coords.add(new Coordinate(n.get(0).asDouble(), n.get(1).asDouble()));
            }

            if (coords.size() < 2) {
                throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
            }

            return GEOMETRY_FACTORY.createLineString(
                coords.toArray(new Coordinate[0])
            );
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
        }
    }

    private LineString parseWkt(String wkt) {
        try {
            Geometry g = new WKTReader(GEOMETRY_FACTORY).read(wkt);
            if (!(g instanceof LineString)) {
                throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
            }
            return (LineString) g;
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.INVALID_ROUTE_PATH);
        }
    }
}