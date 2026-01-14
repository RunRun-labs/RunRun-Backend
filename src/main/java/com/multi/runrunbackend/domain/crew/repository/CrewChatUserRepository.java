package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author : changwoo
 * @description : 크루 채팅방 참여자 Repository
 * @filename : CrewChatUserRepository
 * @since : 2026-01-04
 */
public interface CrewChatUserRepository extends JpaRepository<CrewChatUser, Long> {

    /**
     * 채팅방의 활성 참여자 목록 조회 (크루 정보 포함)
     */
    @Query("SELECT cu FROM CrewChatUser cu " +
            "JOIN FETCH cu.user " +
            "JOIN FETCH cu.room r " +
            "JOIN FETCH r.crew " +
            "WHERE cu.room.id = :roomId AND cu.isDeleted = false")
    List<CrewChatUser> findActiveUsersByRoomId(@Param("roomId") Long roomId);

    /**
     * 특정 유저의 채팅방 참여 정보 조회
     */
    @Query("SELECT cu FROM CrewChatUser cu " +
            "WHERE cu.room.id = :roomId AND cu.user.id = :userId AND cu.isDeleted = false")
    Optional<CrewChatUser> findByRoomIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId);

    /**
     * 채팅방 참여자 수 조회
     */
    @Query("SELECT COUNT(cu) FROM CrewChatUser cu " +
            "WHERE cu.room.id = :roomId AND cu.isDeleted = false")
    Long countActiveUsersByRoomId(@Param("roomId") Long roomId);

    /**
     * 유저가 참여 중인 크루 채팅방 목록 조회
     */
    @Query("SELECT cu FROM CrewChatUser cu " +
            "JOIN FETCH cu.room r " +
            "JOIN FETCH r.crew c " +
            "WHERE cu.user.id = :userId " +
            "AND cu.isDeleted = false " +
            "ORDER BY cu.createdAt DESC")
    List<CrewChatUser> findMyCrewChatRooms(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CrewChatUser cu WHERE cu.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
