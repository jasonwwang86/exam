package cn.jack.exam.dto.examplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExamPlanDetailResponse {

    private final Long id;
    private final String name;
    private final Long paperId;
    private final String paperName;
    private final Integer paperDurationMinutes;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String status;
    private final String remark;
    private final Long effectiveExamineeCount;
    private final Long invalidExamineeCount;
    private final LocalDateTime updatedAt;
}
