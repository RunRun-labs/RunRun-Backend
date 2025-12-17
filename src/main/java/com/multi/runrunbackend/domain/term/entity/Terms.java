package com.multi.runrunbackend.domain.term.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.term.constant.TermsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 서비스 이용약관 정보를 관리하는 엔터티 - 변경 시 새로운 버전을 추가한다
 * @filename : Terms
 * @since : 25. 12. 17. 오후 1:22 수요일
 */
@Entity
@Table(name = "terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TermsType type;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean required;
}