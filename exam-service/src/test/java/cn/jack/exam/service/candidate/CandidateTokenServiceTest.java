package cn.jack.exam.service.candidate;

import cn.jack.exam.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CandidateTokenServiceTest {

    @Autowired
    private CandidateTokenService candidateTokenService;

    @Test
    void shouldIssueAndParseCandidateToken() {
        CandidateTokenService.CandidateIssuedToken issuedToken = candidateTokenService.issueToken(1L, true);

        CandidateTokenService.CandidateTokenClaims claims = candidateTokenService.parseToken(issuedToken.token());

        assertThat(claims.examineeId()).isEqualTo(1L);
        assertThat(claims.profileConfirmed()).isTrue();
        assertThat(claims.expiresAt()).isAfterOrEqualTo(issuedToken.expiresAt().minusSeconds(1));
    }

    @Test
    void shouldRejectTamperedCandidateToken() {
        CandidateTokenService.CandidateIssuedToken issuedToken = candidateTokenService.issueToken(1L, false);
        String token = issuedToken.token();
        String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> candidateTokenService.parseToken(tamperedToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("考生登录已失效或不存在");
    }

    @Test
    void shouldRejectBlankCandidateToken() {
        assertThatThrownBy(() -> candidateTokenService.parseToken(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("考生登录已失效或不存在");
    }
}
