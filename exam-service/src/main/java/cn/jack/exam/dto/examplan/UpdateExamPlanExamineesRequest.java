package cn.jack.exam.dto.examplan;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateExamPlanExamineesRequest {

    @NotNull(message = "考试范围不能为空")
    private List<Long> examineeIds;
}
