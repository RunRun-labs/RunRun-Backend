package com.multi.runrunbackend.domain.course.service;

import static com.multi.runrunbackend.common.exception.dto.ErrorCode.ALREADY_LIKED_COURSE;
import static com.multi.runrunbackend.common.exception.dto.ErrorCode.CANNOT_LIKE_OWN_COURSE;
import static com.multi.runrunbackend.common.exception.dto.ErrorCode.COURSE_NOT_AVAILABLE;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.ExternalApiException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.dto.InMemoryMultipartFile;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.course.constant.CourseStatus;
import com.multi.runrunbackend.domain.course.dto.req.CourseCreateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseUpdateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.course.dto.req.RouteRequestDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseCreateResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseDetailResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseListResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseUpdateResDto;
import com.multi.runrunbackend.domain.course.dto.res.RouteResDto;
import com.multi.runrunbackend.domain.course.dto.res.TmapPedestrianResDto;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.entity.CourseLike;
import com.multi.runrunbackend.domain.course.repository.CourseLikeRepository;
import com.multi.runrunbackend.domain.course.repository.CourseRepository;
import com.multi.runrunbackend.domain.course.repository.CourseRepositoryCustom;
import com.multi.runrunbackend.domain.course.util.GeometryParser;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * @author : kyungsoo
 * @description :
 * @filename : CourseController
 * @since : 2025. 12. 18. Thursday
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final WebClient tmapWebClient;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final FileStorage fileStorage;
    private final CourseRepositoryCustom courseRepositoryCustom;
    private final GeometryParser geometryParser;
    private final CourseLikeRepository courseLikeRepository;

    @Value("${tmap.app-key}")
    private String tmapAppKey;

    @Transactional
    public CourseCreateResDto createCourse(
        CourseCreateReqDto req,
        MultipartFile imageFile,
        CustomUser principal
    ) {
        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        LineString parsedPath = geometryParser.parseLineString(req.getPath());

        LineString cleanedPath = simplifyLineString(parsedPath);

        String imageUrl = resolveImageUrl(
            imageFile,
            FileDomainType.COURSE_IMAGE,
            user.getId()
        );

        String thumbnailUrl = generateThumbnailFromPath(cleanedPath, user.getId());
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            thumbnailUrl = imageUrl != null ? imageUrl : "";
        }

        Course course = Course.create(
            user,
            req,
            cleanedPath,
            imageUrl,
            thumbnailUrl,
            req.getCourseRegisterType()
        );

        Course saved = courseRepository.save(course);
        return CourseCreateResDto.builder()
            .id(saved.getId())
            .build();
    }

    @Transactional
    public CourseUpdateResDto updateCourse(
        CustomUser principal,
        Long courseId,
        CourseUpdateReqDto req,
        MultipartFile imageFile
    ) {
        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ForbiddenException(ErrorCode.COURSE_NOT_ACTIVE);
        }

        if (!course.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.COURSE_FORBIDDEN);
        }

        LineString parsedPath = geometryParser.parseLineString(req.getPath());

        LineString cleanedPath = simplifyLineString(parsedPath);

        String imageUrl = resolveImageUrl(
            imageFile,
            FileDomainType.COURSE_IMAGE,
            user.getId()
        );

        String thumbnailUrl = generateThumbnailFromPath(cleanedPath, user.getId());
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            thumbnailUrl = imageUrl != null ? imageUrl : course.getThumbnailUrl();
        }

        course.update(
            user,
            req,
            cleanedPath,
            imageUrl,
            thumbnailUrl,
            req.getCourseRegisterType()
        );

        return CourseUpdateResDto.from(course);
    }

    @Transactional
    public void deleteCourse(CustomUser principal, Long courseId) {
        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
            ErrorCode.USER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new ForbiddenException(ErrorCode.COURSE_NOT_ACTIVE);
        }
        if (!course.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.COURSE_FORBIDDEN);
        }
        course.delete();
    }

    public CursorPage<CourseListResDto> getCourseList(CustomUser principal, CourseListReqDto req) {

        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        return courseRepositoryCustom.searchCourses(req);

    }

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
        return callPedestrian(
            req.getStartLng(), req.getStartLat(),
            req.getEndLng(), req.getEndLat()
        );
    }


    public RouteResDto roundTrip(RouteRequestDto req) {
        if (req.getDistanceKm() == null || req.getDistanceKm() <= 0) {
            throw new BadRequestException(ErrorCode.ROUTE_DISTANCE_INVALID);
        }

        // 단순: 랜덤 방향으로 (n/2 km) 떨어진 중간점 생성
        double halfMeters = (req.getDistanceKm() * 1000.0) / 2.0;
        LatLng mid = generatePoint(req.getStartLat(), req.getStartLng(), halfMeters);

        RouteResDto go = callPedestrian(
            req.getStartLng(), req.getStartLat(),
            mid.lng, mid.lat
        );
        RouteResDto back = callPedestrian(
            mid.lng, mid.lat,
            req.getStartLng(), req.getStartLat()
        );

        List<double[]> merged = new ArrayList<>();
        merged.addAll(go.getLineString());
        merged.addAll(back.getLineString());

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(go.getTotalDistance() + back.getTotalDistance());
        res.setTotalTime(go.getTotalTime() + back.getTotalTime());
        res.setLineString(merged);
        return res;
    }


    private static final double OVERLAP_EPS = 1e-5;
    private static final double OFFSET_METER = 5.0;
    private static final double SIMPLIFY_TOLERANCE_M = 12.0;
    private static final double LOOP_DIRECT_THRESHOLD_M = 15.0;
    private static final double LOOP_EXTRA_DISTANCE_M = 25.0;
    private static final int LOOP_LOOKAHEAD = 10;
    private static final double MIN_SEGMENT_METERS = 10.0;
    private static final int MAX_SEGMENTS_PER_ROUTE = 80;
    private static final double MAX_ROUTE_METERS = 42195.0;

    private RouteResDto callPedestrian(double startX, double startY, double endX, double endY) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startX", String.valueOf(startX));
        body.put("startY", String.valueOf(startY));
        body.put("endX", String.valueOf(endX));
        body.put("endY", String.valueOf(endY));
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("startName", "출발");
        body.put("endName", "도착");

        TmapPedestrianResDto raw;
        try {
            raw = tmapWebClient.post()
                .uri("/tmap/routes/pedestrian?version=1")
                .header("appKey", tmapAppKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TmapPedestrianResDto.class)
                .block();
        } catch (WebClientResponseException e) {
            log.warn("[CourseService] TMAP 호출 실패(status={}): {} -> {}. fallback straight line 적용",
                e.getStatusCode(), body, e.getMessage());
            return fallbackStraightLine(startX, startY, endX, endY);
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
                            // node = [lng, lat]
                            if (node.isArray() && node.size() >= 2) {
                                double lng = node.get(0).asDouble();
                                double lat = node.get(1).asDouble();
                                coords.add(new double[]{lng, lat});
                            }
                        }
                    }
                }
            }

            // totalDistance/totalTime 추출은 그대로
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

        List<double[]> normalized = normalizeRoute(coords);

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(totalDistance);
        res.setTotalTime(totalTime);
        res.setLineString(normalized);
        return res;
    }

    private RouteResDto fallbackStraightLine(double startX, double startY, double endX,
        double endY) {
        List<double[]> coords = new ArrayList<>();
        coords.add(new double[]{startX, startY});
        coords.add(new double[]{endX, endY});
        double distance = distanceMeters(coords.get(0), coords.get(1));
        double estimatedTimeSec = distance / 1.2;

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(distance);
        res.setTotalTime(estimatedTimeSec);
        res.setLineString(coords);
        return res;
    }


    private LatLng generatePoint(double startLat, double startLng, double meters) {
        double R = 6378137.0;

        // 랜덤 방위각 0~360
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


    private String resolveImageUrl(
        MultipartFile file,
        FileDomainType domainType,
        Long refId
    ) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return fileStorage.upload(file, domainType, refId);
    }

    private String generateThumbnailFromPath(LineString path, Long refId) {

        if (path == null || path.isEmpty()) {
            return null;
        }

        List<double[]> coords = toCoordinateList(path);
        coords = pruneSmallLoops(coords);
        coords = simplifyCoordinates(coords, SIMPLIFY_TOLERANCE_M);

        if (coords.size() < 2) {
            return null;
        }

        List<double[]> sampled = sampleCoordinates(coords, 200);

        byte[] imageBytes = drawPathThumbnail(sampled);
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        MultipartFile file = new InMemoryMultipartFile(
            "thumbnail",
            "thumbnail.png",
            MediaType.IMAGE_PNG_VALUE,
            imageBytes
        );

        return fileStorage.upload(file, FileDomainType.COURSE_THUMBNAIL, refId);
    }


    private byte[] drawPathThumbnail(List<double[]> coords) {
        int width = 400;
        int height = 200;
        int padding = 12;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            double minLng = Double.MAX_VALUE;
            double maxLng = Double.MIN_VALUE;
            double minLat = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE;

            for (double[] c : coords) {
                minLng = Math.min(minLng, c[0]);
                maxLng = Math.max(maxLng, c[0]);
                minLat = Math.min(minLat, c[1]);
                maxLat = Math.max(maxLat, c[1]);
            }

            final double minLngFinal = minLng;
            final double maxLngFinal = maxLng;
            final double minLatFinal = minLat;
            final double maxLatFinal = maxLat;

            final double dx = Math.max(maxLngFinal - minLngFinal, 1e-9);
            final double dy = Math.max(maxLatFinal - minLatFinal, 1e-9);

            Function<double[], Point2D> toPoint = coord -> {
                double lng = coord[0];
                double lat = coord[1];
                double x = padding + (lng - minLngFinal) / dx * (width - padding * 2);
                double y = padding + (maxLatFinal - lat) / dy * (height - padding * 2);
                return new Point2D.Double(x, y);
            };

            Path2D path = new Path2D.Double();
            Point2D first = toPoint.apply(coords.get(0));
            path.moveTo(first.getX(), first.getY());
            for (int i = 1; i < coords.size(); i++) {
                Point2D p = toPoint.apply(coords.get(i));
                path.lineTo(p.getX(), p.getY());
            }

            g2d.setColor(new Color(0xFF3D00));
            g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(path);

            drawMarker(g2d, toPoint.apply(coords.get(0)), Color.BLACK);
            drawMarker(g2d, toPoint.apply(coords.get(coords.size() - 1)), Color.BLACK);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            g2d.dispose();
        }
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
        res.setLineString(normalizeRoute(merged));
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
            if (!almostSameLatLng(reduced.get(reduced.size() - 1), pts.get(idx))) {
                reduced.add(pts.get(idx));
            }
            cursor += step;
        }
        reduced.add(pts.get(pts.size() - 1));
        return reduced;
    }

    private boolean almostSameLatLng(LatLng a, LatLng b) {
        return distanceMeters(a, b) < MIN_SEGMENT_METERS;
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

    private List<double[]> normalizeRoute(List<double[]> coords) {
        if (coords == null) {
            return null;
        }
        List<double[]> mitigated = mitigateOverlap(coords);
        List<double[]> pruned = pruneSmallLoops(mitigated);
        return simplifyCoordinates(pruned, SIMPLIFY_TOLERANCE_M);
    }

    private List<double[]> mitigateOverlap(List<double[]> coords) {
        if (coords == null || coords.size() < 3) {
            return coords;
        }
        List<double[]> adjusted = new ArrayList<>(coords.size() + 4);
        for (int i = 0; i < coords.size(); i++) {
            double[] current = coords.get(i).clone();
            if (!adjusted.isEmpty()) {
                double[] prev = adjusted.get(adjusted.size() - 1);
                if (almostSame(prev, current)) {
                    current[0] += 1e-5;
                    current[1] += 1e-5;
                }
            }

            if (adjusted.size() >= 2) {
                double[] prev = adjusted.get(adjusted.size() - 1);
                double[] prevPrev = adjusted.get(adjusted.size() - 2);
                if (almostSame(prevPrev, current)) {
                    double[] bulged = bulgePoint(prevPrev, prev, OFFSET_METER);
                    if (bulged != null) {
                        adjusted.add(bulged);
                    }
                }
            }

            adjusted.add(current);
        }
        return adjusted;
    }

    private double[] bulgePoint(double[] start, double[] middle, double offsetMeter) {
        double dx = middle[0] - start[0];
        double dy = middle[1] - start[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < OVERLAP_EPS) {
            return null;
        }
        double offsetDeg = offsetMeter / 111320.0;
        double ox = -dy / len * offsetDeg;
        double oy = dx / len * offsetDeg;
        return new double[]{middle[0] + ox, middle[1] + oy};
    }

    private boolean almostSame(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < OVERLAP_EPS && Math.abs(a[1] - b[1]) < OVERLAP_EPS;
    }

    private List<double[]> pruneSmallLoops(List<double[]> coords) {
        if (coords == null || coords.size() < 4) {
            return coords;
        }
        List<double[]> pruned = new ArrayList<>();
        int n = coords.size();
        int i = 0;
        pruned.add(coords.get(0));

        while (i < n - 1) {
            int skipTo = -1;
            int limit = Math.min(n - 1, i + LOOP_LOOKAHEAD);
            for (int j = i + 2; j <= limit; j++) {
                double direct = distanceMeters(coords.get(i), coords.get(j));
                if (direct <= LOOP_DIRECT_THRESHOLD_M) {
                    double pathLen = cumulativeDistance(coords, i, j);
                    if (pathLen - direct >= LOOP_EXTRA_DISTANCE_M) {
                        skipTo = j;
                        break;
                    }
                }
            }
            if (skipTo != -1) {
                i = skipTo;
            } else {
                i++;
            }
            pruned.add(coords.get(i));
        }
        return pruned;
    }

    private double cumulativeDistance(List<double[]> coords, int start, int end) {
        double sum = 0;
        for (int k = start + 1; k <= end; k++) {
            sum += distanceMeters(coords.get(k - 1), coords.get(k));
        }
        return sum;
    }

    private double distanceMeters(double[] a, double[] b) {
        double R = 6371000.0;
        double dLat = Math.toRadians(b[1] - a[1]);
        double dLng = Math.toRadians(b[0] - a[0]);
        double lat1 = Math.toRadians(a[1]);
        double lat2 = Math.toRadians(b[1]);
        double sinLat = Math.sin(dLat / 2);
        double sinLng = Math.sin(dLng / 2);
        double c = sinLat * sinLat + sinLng * sinLng * Math.cos(lat1) * Math.cos(lat2);
        return 2 * R * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
    }

    private List<double[]> simplifyCoordinates(List<double[]> coords, double toleranceMeters) {
        if (coords == null || coords.size() <= 2 || toleranceMeters <= 0) {
            return coords;
        }

        int n = coords.size();
        double originLat = coords.get(0)[1];
        double originLng = coords.get(0)[0];
        double cosLat = Math.cos(Math.toRadians(originLat));

        double[][] projected = new double[n][2];
        for (int i = 0; i < n; i++) {
            double lng = coords.get(i)[0];
            double lat = coords.get(i)[1];
            double x = (lng - originLng) * 111320.0 * cosLat;
            double y = (lat - originLat) * 110540.0;
            projected[i][0] = x;
            projected[i][1] = y;
        }

        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        double toleranceSq = toleranceMeters * toleranceMeters;
        simplifySection(projected, 0, n - 1, keep, toleranceSq);

        List<double[]> simplified = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                simplified.add(coords.get(i));
            }
        }
        return simplified.size() >= 2 ? simplified : coords;
    }

    private void simplifySection(double[][] points, int start, int end, boolean[] keep,
        double toleranceSq) {
        if (end <= start + 1) {
            return;
        }

        double maxDist = 0;
        int index = -1;

        for (int i = start + 1; i < end; i++) {
            double dist = distancePointToSegmentSquared(points[i], points[start], points[end]);
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        if (index != -1 && maxDist > toleranceSq) {
            keep[index] = true;
            simplifySection(points, start, index, keep, toleranceSq);
            simplifySection(points, index, end, keep, toleranceSq);
        }
    }

    private double distancePointToSegmentSquared(double[] p, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        if (dx == 0 && dy == 0) {
            dx = p[0] - a[0];
            dy = p[1] - a[1];
            return dx * dx + dy * dy;
        }
        double t = ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projX = a[0] + t * dx;
        double projY = a[1] + t * dy;
        double diffX = p[0] - projX;
        double diffY = p[1] - projY;
        return diffX * diffX + diffY * diffY;
    }

    private void drawMarker(Graphics2D g2d, Point2D point, Color color) {
        int radius = 4;
        g2d.setColor(color);
        g2d.fillOval(
            (int) Math.round(point.getX() - radius),
            (int) Math.round(point.getY() - radius),
            radius * 2,
            radius * 2
        );
    }

    private List<double[]> toCoordinateList(LineString path) {
        Coordinate[] coordinates = path.getCoordinates();
        List<double[]> list = new ArrayList<>(coordinates.length);
        for (Coordinate coordinate : coordinates) {
            list.add(new double[]{coordinate.getX(), coordinate.getY()});
        }
        return list;
    }

    private LineString simplifyLineString(LineString path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        List<double[]> coords = toCoordinateList(path);
        coords = pruneSmallLoops(coords);
        List<double[]> simplified = simplifyCoordinates(coords, SIMPLIFY_TOLERANCE_M);
        if (simplified == null || simplified.size() < 2) {
            return path;
        }
        Coordinate[] arr = new Coordinate[simplified.size()];
        for (int i = 0; i < simplified.size(); i++) {
            double[] c = simplified.get(i);
            arr[i] = new Coordinate(c[0], c[1]);
        }
        return path.getFactory().createLineString(arr);
    }

    private List<double[]> sampleCoordinates(List<double[]> coords, int maxCount) {
        if (coords.size() <= maxCount) {
            return coords;
        }
        List<double[]> sampled = new ArrayList<>(maxCount);
        double step = (double) (coords.size() - 1) / (maxCount - 1);
        for (int i = 0; i < maxCount; i++) {
            int index = (int) Math.round(i * step);
            sampled.add(coords.get(Math.min(index, coords.size() - 1)));
        }
        return sampled;
    }

    public CourseDetailResDto getCourse(CustomUser principal, Long courseId) {
        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
            ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

        return CourseDetailResDto.fromEntity(course, user);

    }

    public void likeCourse(CustomUser principal, Long courseId) {

        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
            ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new BusinessException(COURSE_NOT_AVAILABLE);
        }
        if (course.getUser().getId().equals(user.getId())) {
            throw new BusinessException(CANNOT_LIKE_OWN_COURSE);
        }

        if (courseLikeRepository.existsByCourse_IdAndUser_Id(user.getId(), courseId)) {
            throw new BusinessException(ALREADY_LIKED_COURSE);
        }
        courseLikeRepository.save(CourseLike.create(user, course));

        courseRepository.increaseLikeCount(courseId);
    }

    public void unLikeCourse(CustomUser principal, Long courseId) {
        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
            ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new BusinessException(COURSE_NOT_AVAILABLE);
        }

        int deleted = courseLikeRepository
            .deleteByCourseIdAndUserId(courseId, user.getId());

        if (deleted == 0) {
            throw new BadRequestException(ErrorCode.NOT_LIKED);
        }

        int updated = courseRepository.decreaseLikeCount(courseId);

        if (updated == 0) {
            log.warn("likeCount already zero. courseId={}", courseId);
        }

    }


    private record LatLng(double lat, double lng) {

    }

}
