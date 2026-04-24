package cn.jack.exam.config;

import cn.jack.exam.common.AuthConstants;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.service.candidate.CandidateAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateAuthInterceptor implements HandlerInterceptor {

    private final CandidateAuthService candidateAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String requestUri = request.getRequestURI();
        if ("/api/candidate/auth/login".equals(requestUri)) {
            return true;
        }

        String authorization = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            log.warn("traceNo={} event=candidate_authentication_missing uri={}",
                    TraceContext.getTraceNo(),
                    requestUri);
            throw new UnauthorizedException("考生登录已失效或不存在");
        }

        String token = authorization.substring(AuthConstants.BEARER_PREFIX.length());
        CandidateUserContextHolder.set(candidateAuthService.loadContextByToken(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CandidateUserContextHolder.clear();
    }
}
