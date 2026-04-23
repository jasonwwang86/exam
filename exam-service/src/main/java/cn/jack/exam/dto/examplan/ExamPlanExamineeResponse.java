package cn.jack.exam.dto.examplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExamPlanExamineeResponse {

    private final Long id;
    private final String examineeNo;
    private final String name;
    private final String status;
}
