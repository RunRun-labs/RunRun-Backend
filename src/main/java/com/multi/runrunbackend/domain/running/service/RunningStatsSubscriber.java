package com.multi.runrunbackend.domain.running.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.running.dto.RunningStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * GPS 통계 Redis Subscriber
 * - Redis 채널에서 GPS 통계 수신
 * - 모든 서버의 WebSocket 클라이언트에게 전달
 * 
 * @author : chang
 * @since : 2024-12-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunningStatsSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Redis에서 GPS 통계 수신 → WebSocket 브로드캐스트
     */
    public void handleMessage(String publishMessage) {
        try {
            log.debug("📥 Redis Sub: GPS 통계 수신");
            
            // JSON → RunningStatsDTO
            RunningStatsDTO stats = objectMapper.readValue(publishMessage, RunningStatsDTO.class);
            
            log.info("📊 GPS 통계 브로드캐스트: sessionId={}, distance={}km", 
                    stats.getSessionId(), 
                    stats.getTotalDistance());
            
            // 해당 세션 구독자들에게 WebSocket으로 전송
            messagingTemplate.convertAndSend(
                    "/sub/running/" + stats.getSessionId(),
                    stats
            );
            
        } catch (Exception e) {
            log.error("❌ GPS 통계 처리 실패: {}", e.getMessage(), e);
        }
    }
}
