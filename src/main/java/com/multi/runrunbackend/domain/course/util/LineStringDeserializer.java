package com.multi.runrunbackend.domain.course.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Custom deserializer that converts a GeoJSON-like object into a JTS LineString.
 */
public class LineStringDeserializer extends StdDeserializer<LineString> {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(
        new PrecisionModel(), 4326
    );

    public LineStringDeserializer() {
        super(LineString.class);
    }

    @Override
    public LineString deserialize(JsonParser parser, DeserializationContext ctxt) {

        JsonNode node;
        try {
            node = ctxt.readTree(parser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (node == null || !node.has("coordinates")) {
            return null;
        }

        JsonNode coordinatesNode = node.get("coordinates");
        if (!coordinatesNode.isArray() || coordinatesNode.isEmpty()) {
            return null;
        }

        List<Coordinate> coordinates = new ArrayList<>();
        for (JsonNode coordinateNode : coordinatesNode) {
            if (!coordinateNode.isArray() || coordinateNode.size() < 2) {
                continue;
            }
            double lng = coordinateNode.get(0).asDouble();
            double lat = coordinateNode.get(1).asDouble();
            coordinates.add(new Coordinate(lng, lat));
        }

        if (coordinates.size() < 2) {
            return null;
        }

        return GEOMETRY_FACTORY.createLineString(
            coordinates.toArray(new Coordinate[0])
        );
    }
}
