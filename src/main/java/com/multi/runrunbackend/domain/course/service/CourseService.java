package com.multi.runrunbackend.domain.course.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.dto.InMemoryMultipartFile;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.course.dto.req.CourseCreateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.course.dto.req.RouteRequestDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseCreateResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseDetailResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseListResDto;
import com.multi.runrunbackend.domain.course.dto.res.RouteResDto;
import com.multi.runrunbackend.domain.course.dto.res.TmapPedestrianResDto;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.repository.CourseRepository;
import com.multi.runrunbackend.domain.course.repository.CourseRepositoryCustom;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseController
 * @since : 2025. 12. 18. Thursday
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final WebClient tmapWebClient;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final FileStorage fileStorage;
    private final CourseRepositoryCustom courseRepositoryCustom;


    @Value("${tmap.app-key}")
    private String tmapAppKey;

    @Transactional
    public CourseCreateResDto create(
        CourseCreateReqDto req,
        MultipartFile imageFile,
        CustomUser principal
    ) {
        String loginId = principal.getLoginId();

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new NotFoundException(
            ErrorCode.USER_NOT_FOUND));

        String imageUrl = resolveImageUrl(
            imageFile,
            FileDomainType.COURSE_IMAGE,
            user.getId()
        );

        String thumbnailUrl = generateThumbnailFromPath(req.getPath(), user.getId());
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            thumbnailUrl = imageUrl != null ? imageUrl : "";
        }

        Course course = Course.create(user, req, imageUrl, thumbnailUrl,
            req.getCourseRegisterType());

        Course saved = courseRepository.save(course);
        return CourseCreateResDto.builder().id(saved.getId()).build();
    }

    public CursorPage<CourseListResDto> getCourses(CustomUser principal, CourseListReqDto req) {

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
            return buildRouteFromWaypoints(waypoints);
        }
        if (req.getEndLat() == null || req.getEndLng() == null) {
            throw new IllegalArgumentException("endLat/endLng 필요");
        }
        return callPedestrian(
            req.getStartLng(), req.getStartLat(),
            req.getEndLng(), req.getEndLat()
        );
    }


    public RouteResDto roundTrip(RouteRequestDto req) {
        if (req.getDistanceKm() == null || req.getDistanceKm() <= 0) {
            throw new IllegalArgumentException("distanceKm 필요 (예: 3, 5)");
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

        TmapPedestrianResDto raw = tmapWebClient.post()
            .uri("/tmap/routes/pedestrian?version=1")
            .header("appKey", tmapAppKey)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TmapPedestrianResDto.class)
            .block();

        if (raw == null || raw.getFeatures() == null) {
            throw new IllegalStateException("TMAP 응답이 비어있음");
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

            throw new IllegalStateException("경로 좌표(LineString)가 없음 (좌표/제공지역 확인)");
        }

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(totalDistance);
        res.setTotalTime(totalTime);
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
            throw new IllegalArgumentException("경로 포인트는 최소 2개 이상 필요합니다.");
        }
        List<double[]> merged = new ArrayList<>();
        double totalDistance = 0;
        double totalTime = 0;

        for (int i = 1; i < waypoints.size(); i++) {
            LatLng prev = waypoints.get(i - 1);
            LatLng curr = waypoints.get(i);
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

        RouteResDto res = new RouteResDto();
        res.setTotalDistance(totalDistance);
        res.setTotalTime(totalTime);
        res.setLineString(merged);
        return res;
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

        return CourseDetailResDto.fromEntity(course);

    }


    private record LatLng(double lat, double lng) {

    }

}
