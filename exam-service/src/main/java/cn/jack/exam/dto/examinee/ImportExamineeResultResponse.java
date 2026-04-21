package cn.jack.exam.dto.examinee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ImportExamineeResultResponse {

    private final int successCount;
    private final int failureCount;
    private final List<ImportExamineeFailureResponse> failures;
}
