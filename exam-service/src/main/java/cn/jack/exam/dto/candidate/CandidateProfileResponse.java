package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CandidateProfileResponse {

    Long examineeId;
    String examineeNo;
    String name;
    String maskedIdCardNo;
    boolean profileConfirmed;
    String message;
}
