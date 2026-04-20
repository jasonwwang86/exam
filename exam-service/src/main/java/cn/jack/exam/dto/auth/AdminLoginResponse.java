package cn.jack.exam.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminLoginResponse {

    private final String token;
    private final String tokenType;
    private final LocalDateTime expiresAt;
    private final AdminLoginUserResponse user;
}
