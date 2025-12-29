package com.multi.runrunbackend.domain.tts.dto.req;

import com.multi.runrunbackend.domain.tts.constant.TtsCue;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsPresignBatchReqDto
 * @since : 2025. 12. 24. Wednesday
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TtsPresignBatchReqDto {

    private Long voicePackId;
    @Size(max = 200, message = "cues는 최대 200개까지 요청 가능합니다.")
    private List<TtsCue> cues;

}
