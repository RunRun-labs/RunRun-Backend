package com.multi.runrunbackend.domain.tts.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 학습 시킨 tts 엔티티 목록 관리를 위해 존재한다(ElvenLabs API에서 받은 id를 넣어줘야한다)
 * @filename : Tts
 * @since : 2025. 12. 17. Wednesday
 */
@Entity
@Table(name = "tts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tts extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String voiceType;
}