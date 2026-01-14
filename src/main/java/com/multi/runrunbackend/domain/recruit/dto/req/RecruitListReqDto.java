package com.multi.runrunbackend.domain.recruit.dto.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @author : KIMGWANGHO
 * @description : 모집글 조회 요청 dto
 * @filename : RecruitListReqDto
 * @since : 2025-12-18 목요일
 */
@Data
public class RecruitListReqDto {

  @DecimalMin("-90.0")
  @DecimalMax("90.0")
  private Double latitude;

  @DecimalMin("-180.0")
  @DecimalMax("180.0")
  private Double longitude;

  @DecimalMin(value = "0.0", inclusive = false)
  private Double radiusKm;

  private String keyword;
  private String sortBy;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate date;

  private String region;

  private Boolean isParticipated;
}