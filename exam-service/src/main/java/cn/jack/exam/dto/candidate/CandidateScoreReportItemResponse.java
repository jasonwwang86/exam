package cn.jack.exam.dto.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CandidateScoreReportItemResponse {

    private Long paperQuestionId;
    private Long questionId;
    private Integer questionNo;
    private String questionStem;
    private String questionTypeName;
    private String answerMode;
    private JsonNode answerConfig;
    private BigDecimal itemScore;
    private BigDecimal awardedScore;
    private String answerStatus;
    private String answerSummary;
    private JsonNode savedAnswer;
    private String judgeStatus;
}
