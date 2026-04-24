package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateExamSubmissionResponse {

    private Long planId;
    private String name;
    private String paperName;
    private String sessionStatus;
    private String submissionMethod;
    private LocalDateTime submittedAt;
    private Integer answeredCount;
    private Integer totalQuestionCount;
}
