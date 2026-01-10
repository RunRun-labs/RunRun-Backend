package com.multi.runrunbackend.domain.chat.repository;

import com.multi.runrunbackend.domain.chat.document.OfflineChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author : changwoo
 * @description : Please explain the class!!!
 * @filename : OfflineChatMessageRepository
 * @since : 2025-12-18 목요일
 */
public interface OfflineChatMessageRepository extends MongoRepository<OfflineChatMessage, String> {

  List<OfflineChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

  List<OfflineChatMessage> findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(Long sessionId,
      LocalDateTime after);

  /**
   * 세션별 최근 메시지 1개 조회
   */
  OfflineChatMessage findTopBySessionIdOrderByCreatedAtDesc(Long sessionId);

  /**
   * 특정 시간 이후의 메시지 개수
   */
  int countBySessionIdAndCreatedAtAfter(Long sessionId, LocalDateTime after);

  /**
   * 세션별 전체 메시지 개수
   */
  int countBySessionId(Long sessionId);

  /**
   * 특정 세션의 모든 메시지 삭제 (디버깅용)
   */
  int deleteBySessionId(Long sessionId);

}

