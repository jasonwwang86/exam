package cn.jack.exam.config;

import cn.jack.exam.entity.Examinee;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CandidateUserContext {

    private final Examinee examinee;
    private final boolean profileConfirmed;
    private final String token;
    private final LocalDateTime expiresAt;
}
