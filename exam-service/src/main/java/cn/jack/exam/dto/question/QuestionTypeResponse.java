package cn.jack.exam.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuestionTypeResponse {

    private final Long id;
    private final String name;
    private final String answerMode;
    private final Integer sort;
    private final String remark;
}
