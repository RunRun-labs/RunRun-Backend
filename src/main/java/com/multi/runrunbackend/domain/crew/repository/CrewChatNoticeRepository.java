package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewChatNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : changwoo
 * @description : 크루 채팅방 공지사항 Repository
 * @filename : CrewChatNoticeRepository
 * @since : 2026-01-05
 */
public interface CrewChatNoticeRepository extends JpaRepository<CrewChatNotice, Long> {

  /**
   * 채팅방별 공지사항 목록 조회 (최신순)
   */
  List<CrewChatNotice> findByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(Long roomId);


}
