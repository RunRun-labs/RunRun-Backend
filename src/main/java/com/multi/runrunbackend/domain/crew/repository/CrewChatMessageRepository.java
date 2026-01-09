package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.document.CrewChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author : changwoo
 * @description : 크루 채팅 메시지 Repository (MongoDB)
 * @filename : CrewChatMessageRepository
 * @since : 2026-01-04
 */
public interface CrewChatMessageRepository extends MongoRepository<CrewChatMessage, String> {


  /**
   * 특정 시간 이후 메시지 조회
   */
  List<CrewChatMessage> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(Long roomId,
      LocalDateTime after);

  /**
   * 채팅방별 최근 메시지 1개 조회
   */
  CrewChatMessage findTopByRoomIdOrderByCreatedAtDesc(Long roomId);


  /**
   * 채팅방별 모든 메시지 삭제
   */
  void deleteByRoomId(Long roomId);

}
