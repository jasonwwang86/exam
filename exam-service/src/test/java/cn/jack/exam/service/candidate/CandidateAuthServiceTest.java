package cn.jack.exam.service.candidate;

import cn.jack.exam.config.CandidateUserContext;
import cn.jack.exam.dto.candidate.CandidateLoginRequest;
import cn.jack.exam.entity.ExamAnswerSession;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamineeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CandidateAuthServiceTest {

    @Autowired
    private CandidateAuthService candidateAuthService;

    @Autowired
    private CandidateTokenService candidateTokenService;

    @Autowired
    private ExamineeMapper examineeMapper;

    @Autowired
    private ExamAnswerSessionMapper examAnswerSessionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoginCandidateAndReturnMaskedProfile() {
        var response = candidateAuthService.login(loginRequest("EX2026001", "110101199001010011"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.isProfileConfirmed()).isFalse();
        assertThat(response.getProfile().getExamineeId()).isEqualTo(1L);
        assertThat(response.getProfile().getMaskedIdCardNo()).isEqualTo("110101********0011");
    }

    @Test
    void shouldLoadContextAndConfirmProfile() {
        var loginResponse = candidateAuthService.login(loginRequest("EX2026001", "110101199001010011"));

        CandidateUserContext context = candidateAuthService.loadContextByToken(loginResponse.getToken());
        var confirmResponse = candidateAuthService.confirmProfile(context);

        assertThat(context.isProfileConfirmed()).isFalse();
        assertThat(context.getExaminee().getId()).isEqualTo(1L);
        assertThat(confirmResponse.isProfileConfirmed()).isTrue();
        assertThat(confirmResponse.getProfile().getMaskedIdCardNo()).isEqualTo("110101********0011");
        assertThat(candidateTokenService.parseToken(confirmResponse.getToken()).profileConfirmed()).isTrue();
    }

    @Test
    void shouldRejectExamListBeforeProfileConfirmation() {
        CandidateUserContext context = CandidateUserContext.builder()
                .examinee(examineeMapper.selectById(1L))
                .profileConfirmed(false)
                .token("candidate-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        assertThatThrownBy(() -> candidateAuthService.listAvailableExams(context))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("请先确认身份信息");
    }

    @Test
    void shouldReturnSubmittedExamSummaryForConfirmedCandidate() {
        ExamAnswerSession session = new ExamAnswerSession();
        LocalDateTime now = LocalDateTime.now();
        session.setExamPlanId(1L);
        session.setExamineeId(1L);
        session.setPaperId(1L);
        session.setStartedAt(now.minusMinutes(20));
        session.setDeadlineAt(now.plusMinutes(100));
        session.setStatus("SUBMITTED");
        session.setSubmittedAt(now.minusMinutes(5));
        session.setCreatedAt(now.minusMinutes(20));
        session.setUpdatedAt(now.minusMinutes(5));
        examAnswerSessionMapper.insert(session);

        CandidateUserContext context = CandidateUserContext.builder()
                .examinee(examineeMapper.selectById(1L))
                .profileConfirmed(true)
                .token("candidate-token")
                .expiresAt(now.plusHours(1))
                .build();

        var exams = candidateAuthService.listAvailableExams(context);

        assertThat(exams).hasSize(1);
        assertThat(exams.getFirst().getPlanId()).isEqualTo(1L);
        assertThat(exams.getFirst().getAnsweringStatus()).isEqualTo("SUBMITTED");
        assertThat(exams.getFirst().getRemainingSeconds()).isZero();
        assertThat(exams.getFirst().getCanEnterAnswering()).isFalse();
        assertThat(exams.getFirst().getSubmissionMethod()).isEqualTo("MANUAL");
    }

    @Test
    void shouldExposeScoreSummaryWhenResultExists() {
        LocalDateTime now = LocalDateTime.now();
        ExamAnswerSession session = new ExamAnswerSession();
        session.setExamPlanId(1L);
        session.setExamineeId(1L);
        session.setPaperId(1L);
        session.setStartedAt(now.minusMinutes(20));
        session.setDeadlineAt(now.plusMinutes(100));
        session.setStatus("SUBMITTED");
        session.setSubmittedAt(now.minusMinutes(5));
        session.setCreatedAt(now.minusMinutes(20));
        session.setUpdatedAt(now.minusMinutes(5));
        examAnswerSessionMapper.insert(session);

        jdbcTemplate.update(
                """
                insert into exam_result (
                    exam_plan_id, examinee_id, session_id, paper_id, score_status, total_score, objective_score, subjective_score,
                    answered_count, unanswered_count, submitted_at, generated_at, published_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L,
                1L,
                session.getId(),
                1L,
                "PUBLISHED",
                new BigDecimal("92.50"),
                new BigDecimal("86.50"),
                new BigDecimal("6.00"),
                2,
                0,
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(1)),
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(1))
        );

        CandidateUserContext context = CandidateUserContext.builder()
                .examinee(examineeMapper.selectById(1L))
                .profileConfirmed(true)
                .token("candidate-token")
                .expiresAt(now.plusHours(1))
                .build();

        var exams = candidateAuthService.listAvailableExams(context);
        Map<String, Object> exam = objectMapper.convertValue(exams.getFirst(), Map.class);

        assertThat(exams).hasSize(1);
        assertThat(exams.getFirst().getPlanId()).isEqualTo(1L);
        assertThat(exams.getFirst().getAnsweringStatus()).isEqualTo("SUBMITTED");
        assertThat(exam).containsEntry("scoreStatus", "PUBLISHED");
        assertThat(exam).containsEntry("reportAvailable", true);
        assertThat(exam.get("totalScore").toString()).isEqualTo("92.50");
        assertThat(exam.get("resultGeneratedAt")).isNotNull();
    }

    @Test
    void shouldRejectInvalidCandidateCredentials() {
        assertThatThrownBy(() -> candidateAuthService.login(loginRequest("EX2026001", "wrong-id-card")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("考生编号或身份证号错误");
    }

    private CandidateLoginRequest loginRequest(String examineeNo, String idCardNo) {
        CandidateLoginRequest request = new CandidateLoginRequest();
        request.setExamineeNo(examineeNo);
        request.setIdCardNo(idCardNo);
        return request;
    }
}
