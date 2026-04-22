package cn.jack.exam.dto.paper;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdatePaperQuestionRequest {

    @NotNull(message = "题目分值必须大于0")
    @DecimalMin(value = "0.01", message = "题目分值必须大于0")
    private BigDecimal itemScore;

    @NotNull(message = "题目顺序必须为正整数")
    @Min(value = 1, message = "题目顺序必须为正整数")
    private Integer displayOrder;
}
