package cn.jack.exam.dto.paper;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePaperQuestionsRequest {

    @NotEmpty(message = "至少选择一道题目")
    private List<@NotNull(message = "题目不能为空") Long> questionIds;
}
