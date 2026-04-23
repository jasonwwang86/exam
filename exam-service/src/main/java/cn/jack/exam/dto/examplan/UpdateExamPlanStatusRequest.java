package cn.jack.exam.dto.examplan;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateExamPlanStatusRequest {

    @NotBlank(message = "考试状态不能为空")
    private String status;
}
