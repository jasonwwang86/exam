package cn.jack.exam.dto.examinee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExamineePageResponse {

    private final long total;
    private final long page;
    private final long pageSize;
    private final List<ExamineeListItemResponse> records;
}
