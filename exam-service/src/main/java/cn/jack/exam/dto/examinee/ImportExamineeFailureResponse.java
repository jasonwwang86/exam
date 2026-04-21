package cn.jack.exam.dto.examinee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ImportExamineeFailureResponse {

    private final int rowNumber;
    private final String message;
}
