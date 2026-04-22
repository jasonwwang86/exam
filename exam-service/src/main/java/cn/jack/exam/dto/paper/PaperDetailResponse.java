package cn.jack.exam.dto.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaperDetailResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Integer durationMinutes;
    private final BigDecimal totalScore;
    private final Long questionCount;
    private final String remark;
    private final LocalDateTime updatedAt;
}
