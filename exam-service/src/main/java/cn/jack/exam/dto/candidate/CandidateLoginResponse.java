package cn.jack.exam.dto.candidate;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CandidateLoginResponse {

    String token;
    String tokenType;
    LocalDateTime expiresAt;
    boolean profileConfirmed;
    CandidateProfileSummaryResponse profile;
}
