package cn.jack.exam.dto.examplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExamPlanListItemResponse {

    private final Long id;
    private final String name;
    private final Long paperId;
    private final String paperName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Long effectiveExamineeCount;
    private final String status;
    private final LocalDateTime updatedAt;
}
