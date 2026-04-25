package cn.jack.exam.config;

import cn.jack.exam.controller.candidate.CandidatePortalController;
import cn.jack.exam.dto.candidate.CandidateLoginRequest;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.service.candidate.CandidateAuthService;
import cn.jack.exam.service.candidate.CandidateAnsweringService;
import cn.jack.exam.service.candidate.CandidateScoreReportService;
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
class CandidateAuthInterceptorTest {

    @Autowired
    private CandidateAuthInterceptor candidateAuthInterceptor;

    @Autowired
    private CandidateAuthService candidateAuthService;

    @AfterEach
    void clearContext() {
        CandidateUserContextHolder.clear();
    }

    @Test
    void shouldAllowCandidateLoginEndpointWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/candidate/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = candidateAuthInterceptor.preHandle(request, response, profileHandler());

        assertThat(allowed).isTrue();
    }

    @Test
    void shouldRejectProtectedCandidateRequestWithoutBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/candidate/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> candidateAuthInterceptor.preHandle(request, response, profileHandler()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("考生登录已失效或不存在");
    }

    @Test
    void shouldSetAndClearCandidateContextForAuthorizedRequest() throws Exception {
        String token = candidateAuthService.login(candidateLoginRequest("EX2026001", "110101199001010011")).getToken();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/candidate/profile");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = candidateAuthInterceptor.preHandle(request, response, profileHandler());

        assertThat(allowed).isTrue();
        assertThat(CandidateUserContextHolder.getRequired().getExaminee().getExamineeNo()).isEqualTo("EX2026001");

        candidateAuthInterceptor.afterCompletion(request, response, profileHandler(), null);
        assertThatThrownBy(CandidateUserContextHolder::getRequired)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("考生登录已失效或不存在");
    }

    private HandlerMethod profileHandler() throws NoSuchMethodException {
        return new HandlerMethod(new CandidatePortalController((CandidateAuthService) null, (CandidateAnsweringService) null, (CandidateScoreReportService) null),
                CandidatePortalController.class.getMethod("profile"));
    }

    private CandidateLoginRequest candidateLoginRequest(String examineeNo, String idCardNo) {
        CandidateLoginRequest request = new CandidateLoginRequest();
        request.setExamineeNo(examineeNo);
        request.setIdCardNo(idCardNo);
        return request;
    }
}
