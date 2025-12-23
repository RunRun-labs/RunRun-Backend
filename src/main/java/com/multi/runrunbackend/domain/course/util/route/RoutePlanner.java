package com.multi.runrunbackend.domain.course.util.route;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.ExternalApiException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.course.client.TmapRouteClient;
import com.multi.runrunbackend.domain.course.dto.req.RouteRequestDto;
import com.multi.runrunbackend.domain.course.dto.res.RouteResDto;
import com.multi.runrunbackend.domain.course.dto.res.TmapPedestrianResDto;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutePlanner {

    private final TmapRouteClient tmapRouteClient;
    private final CoursePathProcessor pathProcessor;

    private static final double MIN_SEGMENT_METERS = 10.0;
    private static final int MAX_SEGMENTS_PER_ROUTE = 80;
    private static final double MAX_ROUTE_METERS = 42195.0;

    public RouteResDto oneWay(RouteRequestDto req) {
        if (req.getViaPoints() != null && req.getViaPoints().size() >= 2) {
            List<LatLng> waypoints = req.getViaPoints().stream()
                .map(p -> new LatLng(p.getLat(), p.getLng()))
                .collect(Collectors.toList());

            double requestedDistance = totalDistanceMeters(waypoints);
            if (requestedDistance > MAX_ROUTE_METERS) {
                throw new BadRequestException(ErrorCode.ROUTE_DISTANCE_EXCEEDED);
            }
            return buildRouteFromWaypoints(waypoints);
        }

        if (req.getEndLat() == null || req.getEndLng() == null) {
            throw new BadRequestException(ErrorCode.ROUTE_END_POINT_REQUIRED);
        }

        double singleDistance = distanceMeters(
            new LatLng(req.getStartLat(), req.getStartLng()),
            new LatLng(req.getEndLat(), req.getEndLng())
        );
        if (singleDistance > MAX_ROUTE_METERS) {
            throw new BadRequestException(ErrorCode.ROUTE_DISTANCE_EXCEEDED);
        }

        return callPedestrian(req.getStartLng(), req.getStartLat(), req.getEndLng(),
            req.getEndLat());
    }

    public RouteResDto roundTrip(RouteRequestDto req) {
        if (req.getDistanceKm() == null || req.getDistanceKm() <= 0) {
            throw new BadRequestException(ErrorCode.ROUTE_DISTANCE_INVALID);
        }

        double halfMeters = (req.getDistanceKm() * 1000.0) / 2.0;
        LatLng mid = generatePoint(req.getStartLat(), req.getStartLng(), halfMeters);

        RouteResDto go = callPedestrian(req.getStartLng(), req.getStartLat(), mid.lng, mid.lat);
        RouteResDto back = callPedestrian(mid.lng, mid.lat, req.getStartLng(), req.getStartLat());

        List<double[]> merged = new ArrayList<>();
        merged.addAll(go.getLineString());
        merged.addAll(back.getLineString());

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(go.getTotalDistance() + back.getTotalDistance());
        res.setTotalTime(go.getTotalTime() + back.getTotalTime());
        res.setLineString(merged);
        return res;
    }

    private RouteResDto callPedestrian(double startLng, double startLat, double endLng,
        double endLat) {
        TmapPedestrianResDto raw;
        try {
            raw = tmapRouteClient.pedestrian(startLng, startLat, endLng, endLat);
        } catch (WebClientResponseException e) {
            log.warn("[RoutePlanner] TMAP 실패(status={}): fallback straight line 적용",
                e.getStatusCode());
            return fallbackStraightLine(startLng, startLat, endLng, endLat);
        } catch (Exception e) {
            log.warn("[RoutePlanner] TMAP 실패: fallback straight line 적용. msg={}", e.getMessage());
            return fallbackStraightLine(startLng, startLat, endLng, endLat);
        }

        if (raw == null || raw.getFeatures() == null) {
            throw new ExternalApiException(ErrorCode.TMAP_EMPTY_RESPONSE);
        }

        List<double[]> coords = new ArrayList<>();
        double totalDistance = 0;
        double totalTime = 0;

        for (TmapPedestrianResDto.Feature f : raw.getFeatures()) {
            var g = f.getGeometry();
            if (g != null && g.getType() != null && g.getCoordinates() != null) {
                if ("LineString".equalsIgnoreCase(g.getType())) {
                    var arr = g.getCoordinates();
                    if (arr.isArray()) {
                        for (var node : arr) {
                            if (node.isArray() && node.size() >= 2) {
                                double lng = node.get(0).asDouble();
                                double lat = node.get(1).asDouble();
                                coords.add(new double[]{lng, lat});
                            }
                        }
                    }
                }
            }

            if (f.getProperties() != null) {
                Object d = f.getProperties().get("totalDistance");
                Object t = f.getProperties().get("totalTime");
                if (d instanceof Number) {
                    totalDistance = ((Number) d).doubleValue();
                }
                if (t instanceof Number) {
                    totalTime = ((Number) t).doubleValue();
                }
            }
        }

        if (coords.isEmpty()) {
            throw new ExternalApiException(ErrorCode.TMAP_NO_ROUTE);
        }

        List<double[]> normalized = pathProcessor.normalizeRoute(coords);

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(totalDistance);
        res.setTotalTime(totalTime);
        res.setLineString(normalized);
        return res;
    }

    private RouteResDto fallbackStraightLine(double startLng, double startLat, double endLng,
        double endLat) {
        List<double[]> coords = new ArrayList<>();
        coords.add(new double[]{startLng, startLat});
        coords.add(new double[]{endLng, endLat});
        double distance = pathProcessor.distanceMeters(coords.get(0), coords.get(1));
        double estimatedTimeSec = distance / 1.2;

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(distance);
        res.setTotalTime(estimatedTimeSec);
        res.setLineString(coords);
        return res;
    }

    private RouteResDto buildRouteFromWaypoints(List<LatLng> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new BadRequestException(ErrorCode.ROUTE_INVALID_POINTS);
        }

        List<LatLng> normalizedWaypoints = enrichWaypoints(waypoints);
        normalizedWaypoints = capWaypoints(normalizedWaypoints);

        List<double[]> merged = new ArrayList<>();
        double totalDistance = 0;
        double totalTime = 0;

        for (int i = 1; i < normalizedWaypoints.size(); i++) {
            LatLng prev = normalizedWaypoints.get(i - 1);
            LatLng curr = normalizedWaypoints.get(i);

            if (distanceMeters(prev, curr) < MIN_SEGMENT_METERS) {
                continue;
            }

            RouteResDto segment = callPedestrian(prev.lng, prev.lat, curr.lng, curr.lat);
            List<double[]> segLine = segment.getLineString();
            if (segLine == null || segLine.isEmpty()) {
                continue;
            }

            if (merged.isEmpty()) {
                merged.addAll(segLine);
            } else {
                merged.addAll(segLine.subList(1, segLine.size()));
            }

            totalDistance += segment.getTotalDistance();
            totalTime += segment.getTotalTime();
        }

        if (merged.isEmpty()) {
            throw new BadRequestException(ErrorCode.ROUTE_NO_VALID_SEGMENT);
        }

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(totalDistance);
        res.setTotalTime(totalTime);
        res.setLineString(pathProcessor.normalizeRoute(merged));
        return res;
    }

    private List<LatLng> enrichWaypoints(List<LatLng> raw) {
        List<LatLng> cleaned = new ArrayList<>();
        LatLng prev = null;

        for (LatLng current : raw) {
            if (prev != null && distanceMeters(prev, current) < 3.0) {
                continue;
            }
            cleaned.add(current);
            prev = current;
        }
        if (cleaned.size() < 2) {
            return cleaned;
        }

        List<LatLng> enriched = new ArrayList<>();
        for (int i = 1; i < cleaned.size(); i++) {
            LatLng start = cleaned.get(i - 1);
            LatLng end = cleaned.get(i);

            enriched.add(start);
            double segment = distanceMeters(start, end);
            if (segment <= 60) {
                continue;
            }

            int steps = (int) Math.ceil(segment / 60.0);
            for (int s = 1; s < steps; s++) {
                double ratio = (double) s / steps;
                enriched.add(new LatLng(
                    start.lat + (end.lat - start.lat) * ratio,
                    start.lng + (end.lng - start.lng) * ratio
                ));
            }
        }
        enriched.add(cleaned.get(cleaned.size() - 1));
        return enriched;
    }

    private List<LatLng> capWaypoints(List<LatLng> pts) {
        if (pts == null || pts.size() <= MAX_SEGMENTS_PER_ROUTE + 1) {
            return pts;
        }

        List<LatLng> reduced = new ArrayList<>();
        reduced.add(pts.get(0));

        double step = (double) (pts.size() - 1) / MAX_SEGMENTS_PER_ROUTE;
        double cursor = step;

        while (cursor < pts.size() - 1) {
            int idx = (int) Math.round(cursor);
            idx = Math.min(Math.max(1, idx), pts.size() - 2);

            if (distanceMeters(reduced.get(reduced.size() - 1), pts.get(idx))
                >= MIN_SEGMENT_METERS) {
                reduced.add(pts.get(idx));
            }
            cursor += step;
        }

        reduced.add(pts.get(pts.size() - 1));
        return reduced;
    }

    private double totalDistanceMeters(List<LatLng> pts) {
        if (pts == null || pts.size() < 2) {
            return 0;
        }
        double sum = 0;
        for (int i = 1; i < pts.size(); i++) {
            sum += distanceMeters(pts.get(i - 1), pts.get(i));
        }
        return sum;
    }

    private double distanceMeters(LatLng a, LatLng b) {
        double R = 6371000.0;
        double dLat = Math.toRadians(b.lat - a.lat);
        double dLng = Math.toRadians(b.lng - a.lng);
        double lat1 = Math.toRadians(a.lat);
        double lat2 = Math.toRadians(b.lat);
        double sinLat = Math.sin(dLat / 2);
        double sinLng = Math.sin(dLng / 2);
        double c = sinLat * sinLat + sinLng * sinLng * Math.cos(lat1) * Math.cos(lat2);
        return 2 * R * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
    }

    private LatLng generatePoint(double startLat, double startLng, double meters) {
        double R = 6378137.0;
        double bearing = Math.random() * 2 * Math.PI;

        double lat1 = Math.toRadians(startLat);
        double lon1 = Math.toRadians(startLng);

        double lat2 = Math.asin(
            Math.sin(lat1) * Math.cos(meters / R) +
                Math.cos(lat1) * Math.sin(meters / R) * Math.cos(bearing)
        );

        double lon2 = lon1 + Math.atan2(
            Math.sin(bearing) * Math.sin(meters / R) * Math.cos(lat1),
            Math.cos(meters / R) - Math.sin(lat1) * Math.sin(lat2)
        );

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    private record LatLng(double lat, double lng) {

    }
}
