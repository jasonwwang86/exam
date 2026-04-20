package cn.jack.exam.service.auth;

import cn.jack.exam.dto.auth.AdminLoginRequest;
import cn.jack.exam.dto.auth.AdminLoginResponse;
import cn.jack.exam.dto.auth.AdminLoginUserResponse;
import cn.jack.exam.dto.auth.AdminCurrentUserResponse;
import cn.jack.exam.dto.permission.AdminMenuItemResponse;
import cn.jack.exam.config.AdminUserContext;
import cn.jack.exam.config.TraceContext;
import cn.jack.exam.common.enums.AdminPermissionType;
import cn.jack.exam.entity.AdminSession;
import cn.jack.exam.entity.AdminPermission;
import cn.jack.exam.entity.AdminRole;
import cn.jack.exam.entity.AdminUser;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.mapper.AdminPermissionMapper;
import cn.jack.exam.mapper.AdminRoleMapper;
import cn.jack.exam.mapper.AdminSessionMapper;
import cn.jack.exam.mapper.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${exam.auth.token-expire-hours:8}")
    private long tokenExpireHours;

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser adminUser = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (adminUser == null) {
            log.warn("traceNo={} event=login_failed username={} reason=invalid_credentials",
                    TraceContext.getTraceNo(),
                    request.getUsername());
            throw new UnauthorizedException("Username or password is invalid");
        }
        if (adminUser.getEnabled() == null || adminUser.getEnabled() != 1) {
            log.warn("traceNo={} event=login_failed username={} reason=account_disabled",
                    TraceContext.getTraceNo(),
                    request.getUsername());
            throw new UnauthorizedException("Username or password is invalid");
        }
        if (!passwordEncoder.matches(request.getPassword(), adminUser.getPasswordHash())) {
            log.warn("traceNo={} event=login_failed username={} reason=invalid_credentials",
                    TraceContext.getTraceNo(),
                    request.getUsername());
            throw new UnauthorizedException("Username or password is invalid");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(tokenExpireHours);
        String token = UUID.randomUUID().toString().replace("-", "");

        AdminSession session = new AdminSession();
        session.setUserId(adminUser.getId());
        session.setToken(token);
        session.setExpiresAt(expiresAt);
        session.setLastActiveAt(now);
        session.setRevoked(0);
        adminSessionMapper.insert(session);

        log.info("traceNo={} event=login_success userId={} username={}",
                TraceContext.getTraceNo(),
                adminUser.getId(),
                adminUser.getUsername());

        return AdminLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .user(AdminLoginUserResponse.builder()
                        .userId(adminUser.getId())
                        .username(adminUser.getUsername())
                        .displayName(adminUser.getDisplayName())
                        .build())
                .build();
    }

    public AdminCurrentUserResponse currentUser(String token) {
        AdminUserContext context = loadContextByToken(token);
        return buildCurrentUserResponse(context);
    }

    public void logout(String token) {
        int updated = adminSessionMapper.update(
                null,
                new LambdaUpdateWrapper<AdminSession>()
                        .eq(AdminSession::getToken, token)
                        .eq(AdminSession::getRevoked, 0)
                        .set(AdminSession::getRevoked, 1)
        );
        if (updated == 0) {
            log.warn("traceNo={} event=logout_failed reason=session_not_found",
                    TraceContext.getTraceNo());
            throw new UnauthorizedException("Authentication is required");
        }
        log.info("traceNo={} event=logout_success", TraceContext.getTraceNo());
    }

    public AdminUserContext loadContextByToken(String token) {
        AdminSession session = adminSessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getToken, token)
                .eq(AdminSession::getRevoked, 0)
                .last("limit 1"));
        if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("traceNo={} event=authentication_failed reason=session_invalid",
                    TraceContext.getTraceNo());
            throw new UnauthorizedException("Authentication is required");
        }

        AdminUser adminUser = adminUserMapper.selectById(session.getUserId());
        if (adminUser == null || adminUser.getEnabled() == null || adminUser.getEnabled() != 1) {
            log.warn("traceNo={} event=authentication_failed reason=user_invalid userId={}",
                    TraceContext.getTraceNo(),
                    session.getUserId());
            throw new UnauthorizedException("Authentication is required");
        }

        List<AdminRole> roles = adminRoleMapper.findByUserId(adminUser.getId());
        List<AdminPermission> permissions = adminPermissionMapper.findByUserId(adminUser.getId());

        return AdminUserContext.builder()
                .adminUser(adminUser)
                .adminSession(session)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    private AdminCurrentUserResponse buildCurrentUserResponse(AdminUserContext context) {
        return AdminCurrentUserResponse.builder()
                .userId(context.getAdminUser().getId())
                .username(context.getAdminUser().getUsername())
                .displayName(context.getAdminUser().getDisplayName())
                .roles(context.getRoles().stream().map(AdminRole::getRoleCode).toList())
                .permissions(context.getPermissions().stream().map(AdminPermission::getPermissionCode).toList())
                .menus(context.getPermissions().stream()
                        .filter(permission -> AdminPermissionType.MENU.name().equals(permission.getPermissionType()))
                        .map(permission -> AdminMenuItemResponse.builder()
                                .code(permission.getPermissionCode())
                                .name(permission.getPermissionName())
                                .path(permission.getPath())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
