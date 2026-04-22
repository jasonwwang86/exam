package cn.jack.exam.dto.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaperQuestionResponse {

    private final Long id;
    private final Long questionId;
    private final String questionStemSnapshot;
    private final String questionTypeNameSnapshot;
    private final String difficultySnapshot;
    private final BigDecimal itemScore;
    private final Integer displayOrder;
    private final LocalDateTime updatedAt;
}
