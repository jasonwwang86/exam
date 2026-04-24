package cn.jack.exam.dto.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CandidateAnswerQuestionItemResponse {

    private Long paperQuestionId;
    private Long questionId;
    private Integer questionNo;
    private String stem;
    private String questionTypeName;
    private String answerMode;
    private JsonNode answerConfig;
    private BigDecimal score;
    private JsonNode savedAnswer;
    private String answerStatus;
}
