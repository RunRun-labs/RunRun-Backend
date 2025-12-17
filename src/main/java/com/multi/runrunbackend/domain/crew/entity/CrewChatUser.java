package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : chang
 * @description :크루채팅방 참여자 정보
 * @filename : CrewChatUser
 * @since : 2025-12-17 수요일
 */
@Entity
@Table(name = "crew_chat_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewChatUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private CrewChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


}