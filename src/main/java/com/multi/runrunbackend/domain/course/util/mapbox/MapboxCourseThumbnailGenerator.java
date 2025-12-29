package com.multi.runrunbackend.domain.course.util.mapbox;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.InvalidRequestException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.dto.InMemoryMultipartFile;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.course.util.route.CoursePathProcessor;
import com.multi.runrunbackend.domain.course.util.thumbnail.LocalDrawThumbnailFallback;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class MapboxCourseThumbnailGenerator {

    private final FileStorage fileStorage;
    private final CoursePathProcessor pathProcessor;
    private final LocalDrawThumbnailFallback localFallback;

    @Qualifier("mapboxWebClient")
    private final WebClient mapboxWebClient;

    // ===== Mapbox Static Images =====
    @Value("${mapbox.access-token}")
    private String mapboxAccessToken;

    @Value("${mapbox.username:mapbox}")
    private String mapboxUsername;

    @Value("${mapbox.style-id:streets-v12}")
    private String mapboxStyleId;

    @Value("${mapbox.thumbnail.width:400}")
    private int width;

    @Value("${mapbox.thumbnail.height:200}")
    private int height;

    @Value("${mapbox.thumbnail.padding:24}")
    private int padding;

    @Value("${mapbox.thumbnail.stroke-width:6}")
    private int strokeWidth;

    @Value("${mapbox.thumbnail.marker-radius:4}")
    private int markerRadius;

    @Value("${mapbox.response.max-bytes:2097152}")
    private int maxBytes;

    // URL 길이 제한 대비
    private static final int MAPBOX_URL_SAFE_LIMIT = 7000;

    /**
     * Mapbox로 썸네일 생성 후 업로드. 실패하면 로컬 draw fallback.
     */
    public String generateAndUpload(LineString path, Long refId) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        List<double[]> coords = pathProcessor.prepareThumbnailPath(path);
        if (coords.size() < 2) {
            return null;
        }

        byte[] bytes = null;

        try {
            // URL 길이 제한 대응 샘플링
            int maxPoints = Math.min(300, coords.size());
            String overlay = null;

            while (maxPoints >= 20) {
                List<double[]> sampled = pathProcessor.sampleCoordinates(coords, maxPoints);
                overlay = buildGeoJsonOverlay(sampled);

                if (overlay.length() <= MAPBOX_URL_SAFE_LIMIT) {
                    break;
                }
                maxPoints = maxPoints / 2;
            }

            if (overlay == null) {
                overlay = buildGeoJsonOverlay(pathProcessor.sampleCoordinates(coords, 80));
            }

            bytes = fetchStaticPngBytes(overlay);
            if (bytes != null && bytes.length > 0 && bytes.length <= maxBytes) {
                return upload(bytes, refId);
            }
        } catch (Exception e) {
            log.warn(
                "[MapboxCourseThumbnailGenerator] mapbox failed. fallback to local draw. msg={}",
                e.getMessage());
        }

        // fallback
        bytes = localFallback.draw(coords, width, height, padding, strokeWidth, markerRadius);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return upload(bytes, refId);
    }

    private String upload(byte[] bytes, Long refId) {
        MultipartFile file = new InMemoryMultipartFile(
            "thumbnail",
            "thumbnail.png",
            MediaType.IMAGE_PNG_VALUE,
            bytes
        );
        return fileStorage.upload(file, FileDomainType.COURSE_THUMBNAIL, refId);
    }

    /**
     * ✅ 두 겹 route(그림자 + 메인) + start/end 점. - 첫 번째 LineString: shadow (검정/투명 + 굵게) - 두 번째
     * LineString: main (주황) - start: green / end: red
     */
    private String buildGeoJsonOverlay(List<double[]> coords) {
        double[] start = coords.get(0);
        double[] end = coords.get(coords.size() - 1);

        String lineCoords = coords.stream()
            .map(c -> "[" + fmt6(c[0]) + "," + fmt6(c[1]) + "]")
            .collect(Collectors.joining(","));

        int shadowWidth = Math.max(strokeWidth + 6, strokeWidth * 2);

        String geojson =
            "{\"type\":\"FeatureCollection\",\"features\":[" +
                // shadow
                "{\"type\":\"Feature\",\"properties\":{" +
                "\"stroke\":\"#000000\",\"stroke-width\":" + shadowWidth
                + ",\"stroke-opacity\":0.25" +
                "},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[" + lineCoords + "]}}," +
                // main
                "{\"type\":\"Feature\",\"properties\":{" +
                "\"stroke\":\"#FF3D00\",\"stroke-width\":" + strokeWidth + ",\"stroke-opacity\":1" +
                "},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[" + lineCoords + "]}}," +
                // start
                "{\"type\":\"Feature\",\"properties\":{" +
                "\"marker-color\":\"#00C853\",\"marker-size\":\"medium\"" +
                "},\"geometry\":{\"type\":\"Point\",\"coordinates\":[" + fmt6(start[0]) + ","
                + fmt6(start[1]) + "]}}," +
                // end
                "{\"type\":\"Feature\",\"properties\":{" +
                "\"marker-color\":\"#D50000\",\"marker-size\":\"medium\"" +
                "},\"geometry\":{\"type\":\"Point\",\"coordinates\":[" + fmt6(end[0]) + "," + fmt6(
                end[1]) + "]}}" +
                "]}";

        String encoded = uriComponentEncode(geojson);
        return "geojson(" + encoded + ")";
    }

    private String fmt6(double v) {
        return String.format(Locale.US, "%.6f", v);
    }

    private String uriComponentEncode(String raw) {
        // URLEncoder는 space를 +로 바꾸므로 %20으로 교정
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private byte[] fetchStaticPngBytes(String overlay) {
        if (mapboxAccessToken == null || mapboxAccessToken.isBlank()) {
            throw new BusinessException(ErrorCode.MAPBOX_ACCESS_TOKEN_EMPTY);
        }
        if (overlay == null || overlay.isBlank()) {
            throw new InvalidRequestException(ErrorCode.MAPBOX_OVERLAY_EMPTY);
        }

        // /styles/v1/{username}/{style_id}/static/{overlay}/auto/{w}x{h}
        String path = String.format(
            "/styles/v1/%s/%s/static/%s/auto/%dx%d",
            mapboxUsername,
            mapboxStyleId,
            overlay,
            width,
            height
        );

        String uri = String.format(
            "/styles/v1/%s/%s/static/%s/auto/%dx%d" +
                "?access_token=%s&padding=%d&attribution=false&logo=false",
            mapboxUsername,
            mapboxStyleId,
            overlay,            // overlay는 geojson(...) 형태, 내부는 이미 1번만 인코딩된 상태
            width,
            height,
            mapboxAccessToken,
            padding
        );

        return mapboxWebClient.get()
            .uri(uri)
            .accept(MediaType.IMAGE_PNG)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }
}
