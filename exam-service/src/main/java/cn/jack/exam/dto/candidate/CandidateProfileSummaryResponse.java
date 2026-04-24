package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CandidateProfileSummaryResponse {

    Long examineeId;
    String examineeNo;
    String name;
    String maskedIdCardNo;
}
