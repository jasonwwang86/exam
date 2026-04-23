package cn.jack.exam.dto.examplan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SaveExamPlanRequest {

    @NotBlank(message = "考试计划名称不能为空")
    @Size(max = 128, message = "考试计划名称长度不能超过128")
    private String name;

    @NotNull(message = "试卷不能为空")
    private Long paperId;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
