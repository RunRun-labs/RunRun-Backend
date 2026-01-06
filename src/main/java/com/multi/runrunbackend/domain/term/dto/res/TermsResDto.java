package com.multi.runrunbackend.domain.term.dto.res;

import com.multi.runrunbackend.domain.term.constant.TermsType;
import com.multi.runrunbackend.domain.term.entity.Terms;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : 이용약관 응답 객체
 * @filename : TermsResDto
 * @since : 25. 12. 29. 오전 10:04 월요일
 */
@Getter
@Builder
@AllArgsConstructor
public class TermsResDto {
    private Long id;
    private TermsType type;
    private String version;
    private String title;
    private String content;
    private boolean required;
    private LocalDateTime createdAt;

    public static TermsResDto from(Terms terms) {
        return TermsResDto.builder()
                .id(terms.getId())
                .type(terms.getType())
                .version(terms.getVersion())
                .title(terms.getTitle())
                .content(terms.getContent())
                .required(terms.isRequired())
                .createdAt(terms.getCreatedAt()) // BaseTimeEntity 상속 가정
                .build();
    }
}