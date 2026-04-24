package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateSaveAnswerResponse {

    private Long paperQuestionId;
    private String answerStatus;
    private LocalDateTime lastSavedAt;
    private Long remainingSeconds;
    private String sessionStatus;
    private Integer answeredCount;
}
