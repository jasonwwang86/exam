package cn.jack.exam.dto.candidate;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandidateAnswerQuestionView {

    private Long paperQuestionId;
    private Long questionId;
    private Integer questionNo;
    private String stem;
    private String questionTypeName;
    private String answerMode;
    private String answerConfig;
    private BigDecimal score;
}
