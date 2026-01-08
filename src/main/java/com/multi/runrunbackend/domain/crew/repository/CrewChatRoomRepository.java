package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * @author : changwoo
 * @description : 크루 채팅방 Repository
 * @filename : CrewChatRoomRepository
 * @since : 2026-01-04
 */
public interface CrewChatRoomRepository extends JpaRepository<CrewChatRoom, Long> {

  /**
   * 크루 ID로 채팅방 조회
   */
  @Query("SELECT cr FROM CrewChatRoom cr " +
      "WHERE cr.crew.id = :crewId AND cr.isDeleted = false")
  Optional<CrewChatRoom> findByCrewId(@Param("crewId") Long crewId);

}
