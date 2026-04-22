package cn.jack.exam.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuestionPageResponse {

    private final long total;
    private final long page;
    private final long pageSize;
    private final List<QuestionListItemResponse> records;
}
