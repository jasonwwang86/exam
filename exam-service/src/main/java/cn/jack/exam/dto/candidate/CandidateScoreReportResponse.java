package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidateScoreReportResponse {

    private Long planId;
    private String name;
    private String paperName;
    private Integer durationMinutes;
    private String remark;
    private String scoreStatus;
    private BigDecimal totalScore;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private Integer answeredCount;
    private Integer unansweredCount;
    private LocalDateTime submittedAt;
    private LocalDateTime generatedAt;
    private LocalDateTime publishedAt;
    private String submissionMethod;
    private List<CandidateScoreReportItemResponse> items;
}
