package cn.jack.exam.dto.auth;

import cn.jack.exam.dto.permission.AdminMenuItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminCurrentUserResponse {

    private final Long userId;
    private final String username;
    private final String displayName;
    private final List<String> roles;
    private final List<String> permissions;
    private final List<AdminMenuItemResponse> menus;
}
