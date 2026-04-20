package cn.jack.exam.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminLoginUserResponse {

    private final Long userId;
    private final String username;
    private final String displayName;
}
