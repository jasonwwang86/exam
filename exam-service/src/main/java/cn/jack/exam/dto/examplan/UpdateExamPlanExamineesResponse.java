package cn.jack.exam.dto.examplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UpdateExamPlanExamineesResponse {

    private final Long planId;
    private final Long effectiveExamineeCount;
}
