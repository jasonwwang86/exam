package cn.jack.exam.dto.candidate;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CandidateAvailableExamResponse {

    private Long planId;
    private String name;
    private String paperName;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String displayStatus;
    private String remark;
    private Boolean canEnterAnswering;
    private String answeringStatus;
    private Long remainingSeconds;
}
