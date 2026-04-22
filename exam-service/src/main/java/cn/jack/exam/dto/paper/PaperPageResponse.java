package cn.jack.exam.dto.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PaperPageResponse {

    private final long total;
    private final long page;
    private final long pageSize;
    private final List<PaperListItemResponse> records;
}
