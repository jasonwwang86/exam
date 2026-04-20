package cn.jack.exam.controller.admin;

import cn.jack.exam.common.AuthConstants;
import cn.jack.exam.dto.auth.AdminCurrentUserResponse;
import cn.jack.exam.dto.auth.AdminLoginRequest;
import cn.jack.exam.dto.auth.AdminLoginResponse;
import cn.jack.exam.service.auth.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }

    @GetMapping("/me")
    public AdminCurrentUserResponse me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return adminAuthService.currentUser(extractToken(authorization));
    }

    @PostMapping("/logout")
    public java.util.Map<String, Object> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        adminAuthService.logout(extractToken(authorization));
        return java.util.Map.of("success", true);
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            throw new cn.jack.exam.exception.UnauthorizedException("Authentication is required");
        }
        return authorization.substring(AuthConstants.BEARER_PREFIX.length());
    }
}
