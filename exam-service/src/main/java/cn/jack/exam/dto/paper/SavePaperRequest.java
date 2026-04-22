package cn.jack.exam.dto.paper;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavePaperRequest {

    @NotBlank(message = "试卷名称不能为空")
    @Size(max = 128, message = "试卷名称长度不能超过128")
    private String name;

    @Size(max = 500, message = "试卷说明长度不能超过500")
    private String description;

    @NotNull(message = "考试时长必须为正整数")
    @Min(value = 1, message = "考试时长必须为正整数")
    private Integer durationMinutes;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
