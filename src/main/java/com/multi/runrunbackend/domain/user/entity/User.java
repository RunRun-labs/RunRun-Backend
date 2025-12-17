package com.multi.runrunbackend.domain.user.entity;


import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : kyungsoo
 * @description : 사용자 엔티티
 * @filename : User
 * @since : 2025. 12. 17. Wednesday
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "login_id"),
        @UniqueConstraint(columnNames = "email")
    }
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // 회원번호

    @Column(name = "login_id", nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;  // 비밀번호(암호화)

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Column(name = "email", nullable = false)
    private String email;

    @Setter
    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "name", nullable = false, length = 4)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;


    @Column(name = "gender", length = 1, nullable = false)
    private String gender;


    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;


    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;


    public void updateLastLogin(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public static User toEntity(UserSignUpDto dto) {
        return User.builder()
            .loginId(dto.getLoginId())
            .password(dto.getUserPassword())
            .email(dto.getUserEmail())
            .name(dto.getUserName())
            .gender(dto.getGender())
            .birthDate(dto.getBirthDate())
            .heightCm(dto.getHeightCm())
            .weightKg(dto.getWeightKg())
            .role("ROLE_USER")
            .build();
    }
}
