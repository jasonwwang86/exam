package cn.jack.exam.config;

import cn.jack.exam.entity.AdminPermission;
import cn.jack.exam.entity.AdminRole;
import cn.jack.exam.entity.AdminSession;
import cn.jack.exam.entity.AdminUser;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminUserContext {

    private final AdminUser adminUser;
    private final AdminSession adminSession;
    private final List<AdminRole> roles;
    private final List<AdminPermission> permissions;
}
