package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.entity.User;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 프로필 조회 응답 DTO
 * - User 엔터티를 외부로 직접 노출하지 않기 위한 변환 객체
 * @filename : ProfileResDto
 * @since : 25. 12. 17. 오후 5:26 수요일
 */
@Getter
public class ProfileResDto {

    private final String loginId;            // 아이디 (수정 불가)
    private final String profileImageUrl;    // 프로필 사진
    private final Integer heightCm;           // 키
    private final Integer weightKg;           // 몸무게

    private ProfileResDto(
            String loginId,
            String profileImageUrl,
            Integer heightCm,
            Integer weightKg
    ) {
        this.loginId = loginId;
        this.profileImageUrl = profileImageUrl;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
    }

    public static ProfileResDto from(User user) {
        return new ProfileResDto(
                user.getLoginId(),
                user.getProfileImageUrl(),
                user.getHeightCm(),
                user.getWeightKg()
        );
    }
}