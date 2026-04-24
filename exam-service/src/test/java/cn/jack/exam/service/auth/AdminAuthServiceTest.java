package cn.jack.exam.service.auth;

import cn.jack.exam.dto.auth.AdminLoginRequest;
import cn.jack.exam.entity.AdminSession;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.mapper.AdminSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminAuthServiceTest {

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private AdminSessionMapper adminSessionMapper;

    @Test
    void shouldLoginAndPersistSession() {
        var response = adminAuthService.login(loginRequest("admin", "Admin@123456"));

        AdminSession session = adminSessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getToken, response.getToken())
                .last("limit 1"));

        assertThat(response.getToken()).hasSize(32);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUserId()).isEqualTo(1L);
        assertThat(response.getUser().getUsername()).isEqualTo("admin");
        assertThat(session).isNotNull();
        assertThat(session.getUserId()).isEqualTo(1L);
        assertThat(session.getRevoked()).isZero();
    }

    @Test
    void shouldLoadCurrentUserFromToken() {
        var response = adminAuthService.login(loginRequest("admin", "Admin@123456"));

        var currentUser = adminAuthService.currentUser(response.getToken());

        assertThat(currentUser.getUserId()).isEqualTo(1L);
        assertThat(currentUser.getUsername()).isEqualTo("admin");
        assertThat(currentUser.getRoles()).contains("SUPER_ADMIN");
        assertThat(currentUser.getPermissions()).contains("dashboard:read");
        assertThat(currentUser.getMenus()).extracting(menu -> menu.getPath()).contains("/dashboard");
    }

    @Test
    void shouldRejectInvalidPassword() {
        assertThatThrownBy(() -> adminAuthService.login(loginRequest("admin", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Username or password is invalid");
    }

    @Test
    void shouldRevokeSessionOnLogout() {
        var response = adminAuthService.login(loginRequest("admin", "Admin@123456"));

        adminAuthService.logout(response.getToken());

        AdminSession session = adminSessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getToken, response.getToken())
                .last("limit 1"));
        assertThat(session).isNotNull();
        assertThat(session.getRevoked()).isEqualTo(1);
        assertThatThrownBy(() -> adminAuthService.loadContextByToken(response.getToken()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication is required");
    }

    private AdminLoginRequest loginRequest(String username, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
