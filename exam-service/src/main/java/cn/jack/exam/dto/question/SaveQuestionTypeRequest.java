package cn.jack.exam.dto.question;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveQuestionTypeRequest {

    @NotBlank(message = "题型名称不能为空")
    @Size(max = 64, message = "题型名称长度不能超过64")
    private String name;

    @NotBlank(message = "答案模式不能为空")
    @Pattern(regexp = "SINGLE_CHOICE|MULTIPLE_CHOICE|TRUE_FALSE|TEXT", message = "答案模式取值不合法")
    private String answerMode;

    @NotNull(message = "排序不能为空")
    @Max(value = 9999, message = "排序取值不合法")
    private Integer sort;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
