package cn.jack.exam.dto.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaperListItemResponse {

    private final Long id;
    private final String name;
    private final BigDecimal totalScore;
    private final Integer durationMinutes;
    private final Long questionCount;
    private final LocalDateTime updatedAt;
}
