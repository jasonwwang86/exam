package cn.jack.exam.dto.question;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class QuestionDetailResponse {

    private final Long id;
    private final String stem;
    private final Long questionTypeId;
    private final String questionTypeName;
    private final String answerMode;
    private final String difficulty;
    private final BigDecimal score;
    private final JsonNode answerConfig;
    private final LocalDateTime updatedAt;
}
