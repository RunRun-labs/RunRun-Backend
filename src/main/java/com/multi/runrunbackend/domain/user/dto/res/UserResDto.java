package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 정보를 클라이언트에 전달하기 위한 응답 DTO. 프로필 정보 조회 시 사용
 * @filename : UserMeResDto
 * @since : 25. 12. 18. 오후 4:26 목요일
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResDto {

    private Long id;
    private String loginId;
    private String email;
    private String name;
    private String role;

    private String profileImageUrl;
    private String gender;
    private LocalDate birthDate;
    private Integer heightCm;
    private Integer weightKg;

    private LocalDateTime lastLoginAt;

    public static UserResDto from(User user) {

        boolean isDeleted = user.getIsDeleted();
        return UserResDto.builder()
            .id(user.getId())
            .loginId(user.getLoginId())
            .email(isDeleted ? null : user.getEmail())
            .name(isDeleted ? "탈퇴한 회원입니다" : user.getName())
            .role(user.getRole())
            .profileImageUrl(isDeleted ? null : user.getProfileImageUrl())
            .gender(isDeleted ? null : user.getGender())
            .birthDate(isDeleted ? null : user.getBirthDate())
            .heightCm(isDeleted ? null : user.getHeightCm())
            .weightKg(isDeleted ? null : user.getWeightKg())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}