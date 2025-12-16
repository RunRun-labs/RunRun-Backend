package com.multi.runrunbackend.domain.user.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignUpDto {

    @NotBlank(message = "아이디는 필수 입력 사항입니다")
    @Size(min = 5, max = 10, message = "아이디는 최대 15자여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 사항입니다")
    @Size(min= 8,max = 18, message = "비밀번호는 최대 18자여야 합니다.")
    private String userPassword;

    @NotBlank(message = "이름은 필수 입력 사항입니다")
    @Size(max = 4, message = "이름은 최대 4자여야 합니다.")
    private String userName;

    @Email(message = "이메일 형식을 유지해야 합니다")
    @NotBlank(message = "이메일은 필수 입력 사항입니다")
    private String userEmail;

    @NotNull(message = "생년월일은 필수 입력 사항입니다.")
    @PastOrPresent(message = "생년월일은 오늘 또는 과거 날짜만 입력할 수 있습니다.")
    private LocalDate birthDate;

    @NotBlank(message = "성별은 필수 입력 사항입니다.")
    @Pattern(regexp = "^(M|F)$", message = "성별은 MALE 또는 FEMALE만 입력 가능합니다.")
    private String gender;

    @Min(value = 50, message = "키는 50cm 이상이어야 합니다.")
    @Max(value = 300, message = "키는 300cm 이하이어야 합니다.")
    private Integer heightCm;

    @Min(value = 10, message = "몸무게는 10kg 이상이어야 합니다.")
    @Max(value = 500, message = "몸무게는 500kg 이하이어야 합니다.")
    private Integer weightKg;
}