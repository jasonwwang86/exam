package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidateAnswerSessionResponse {

    private Long planId;
    private String name;
    private String paperName;
    private Integer durationMinutes;
    private String sessionStatus;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private Long remainingSeconds;
    private Integer answeredCount;
    private Integer totalQuestionCount;
    private LocalDateTime submittedAt;
    private String submissionMethod;
    private List<CandidateAnswerQuestionItemResponse> questions;
}
