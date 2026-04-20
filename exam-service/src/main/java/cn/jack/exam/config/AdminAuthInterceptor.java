package cn.jack.exam.config;

import cn.jack.exam.common.AuthConstants;
import cn.jack.exam.entity.AdminPermission;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.service.auth.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminAuthService adminAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String requestUri = request.getRequestURI();
        if ("/api/admin/auth/login".equals(requestUri)) {
            return true;
        }

        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            log.warn("traceNo={} event=authentication_missing uri={}",
                    TraceContext.getTraceNo(),
                    requestUri);
            throw new UnauthorizedException("Authentication is required");
        }

        String token = authorization.substring(AuthConstants.BEARER_PREFIX.length());
        AdminUserContext context = adminAuthService.loadContextByToken(token);
        AdminUserContextHolder.set(context);

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission != null) {
            Set<String> permissionCodes = context.getPermissions().stream()
                    .map(AdminPermission::getPermissionCode)
                    .collect(Collectors.toSet());
            if (!permissionCodes.contains(requirePermission.value())) {
                log.warn("traceNo={} event=permission_denied userId={} permission={} uri={}",
                        TraceContext.getTraceNo(),
                        context.getAdminUser().getId(),
                        requirePermission.value(),
                        requestUri);
                throw new ForbiddenException("Permission denied");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminUserContextHolder.clear();
    }
}
