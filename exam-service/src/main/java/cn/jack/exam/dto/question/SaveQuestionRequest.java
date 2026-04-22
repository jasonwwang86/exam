package cn.jack.exam.dto.question;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaveQuestionRequest {

    @NotBlank(message = "题干不能为空")
    @Size(max = 1000, message = "题干长度不能超过1000")
    private String stem;

    @NotNull(message = "题型不能为空")
    private Long questionTypeId;

    @NotBlank(message = "难度不能为空")
    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "难度取值不合法")
    private String difficulty;

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.01", message = "分值必须大于0")
    @Digits(integer = 4, fraction = 2, message = "分值格式不正确")
    private BigDecimal score;

    @NotNull(message = "答案配置不能为空")
    private JsonNode answerConfig;
}
