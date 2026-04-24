package cn.jack.exam.config;

import cn.jack.exam.controller.admin.AdminDashboardController;
import cn.jack.exam.dto.auth.AdminLoginRequest;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.service.auth.AdminAuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdminAuthInterceptorTest {

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    private AdminAuthService adminAuthService;

    @AfterEach
    void clearContext() {
        AdminUserContextHolder.clear();
    }

    @Test
    void shouldAllowAdminLoginEndpointWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = adminAuthInterceptor.preHandle(request, response, dashboardHandler());

        assertThat(allowed).isTrue();
    }

    @Test
    void shouldRejectProtectedAdminRequestWithoutBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard/summary");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> adminAuthInterceptor.preHandle(request, response, dashboardHandler()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication is required");
    }

    @Test
    void shouldSetAndClearAdminContextForAuthorizedRequest() throws Exception {
        String token = adminAuthService.login(adminLoginRequest("admin", "Admin@123456")).getToken();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = adminAuthInterceptor.preHandle(request, response, dashboardHandler());

        assertThat(allowed).isTrue();
        assertThat(AdminUserContextHolder.get()).isNotNull();
        assertThat(AdminUserContextHolder.get().getAdminUser().getUsername()).isEqualTo("admin");

        adminAuthInterceptor.afterCompletion(request, response, dashboardHandler(), null);
        assertThat(AdminUserContextHolder.get()).isNull();
    }

    @Test
    void shouldRejectAdminRequestWhenPermissionIsMissing() {
        String token = adminAuthService.login(adminLoginRequest("limited-admin", "Admin@123456")).getToken();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> adminAuthInterceptor.preHandle(request, response, dashboardHandler()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Permission denied");
    }

    private HandlerMethod dashboardHandler() throws NoSuchMethodException {
        return new HandlerMethod(new AdminDashboardController(),
                AdminDashboardController.class.getMethod("summary"));
    }

    private AdminLoginRequest adminLoginRequest(String username, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
