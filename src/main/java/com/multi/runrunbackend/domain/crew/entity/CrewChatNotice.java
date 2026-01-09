package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : changwoo
 * @description : 크루 채팅방 공지사항
 * @filename : CrewChatNotice
 * @since : 2026-01-05
 */
@Entity
@Table(name = "crew_chat_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewChatNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private CrewChatRoom room;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // ===== 비즈니스 로직 =====

    /**
     * 공지사항 생성
     */
    public static CrewChatNotice create(CrewChatRoom room, User user, String content) {
        return CrewChatNotice.builder()
                .room(room)
                .createdBy(user)
                .content(content)
                .build();
    }

    /**
     * 공지사항 수정
     */
    public void update(String content) {
        this.content = content;
    }
}
