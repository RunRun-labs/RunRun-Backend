package com.multi.runrunbackend.domain.user.dto.res;

import com.multi.runrunbackend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : Please explain the class!!!
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
        return UserResDto.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}