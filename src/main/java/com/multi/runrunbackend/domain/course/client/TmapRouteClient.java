package com.multi.runrunbackend.domain.course.client;

import com.multi.runrunbackend.domain.course.dto.res.TmapPedestrianResDto;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TmapRouteClient {

    private final WebClient tmapWebClient;

    @Value("${tmap.app-key}")
    private String tmapAppKey;

    public TmapPedestrianResDto pedestrian(double startLng, double startLat, double endLng,
        double endLat) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startX", String.valueOf(startLng));
        body.put("startY", String.valueOf(startLat));
        body.put("endX", String.valueOf(endLng));
        body.put("endY", String.valueOf(endLat));
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("startName", "출발");
        body.put("endName", "도착");

        return tmapWebClient.post()
            .uri("/tmap/routes/pedestrian?version=1")
            .header("appKey", tmapAppKey)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TmapPedestrianResDto.class)
            .block();
    }
}