package cn.jack.exam.service.candidate;

import cn.jack.exam.config.CandidateUserContext;
import cn.jack.exam.dto.candidate.CandidateSaveAnswerRequest;
import cn.jack.exam.entity.ExamAnswerSession;
import cn.jack.exam.entity.ExamResult;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamResultItemMapper;
import cn.jack.exam.mapper.ExamResultMapper;
import cn.jack.exam.mapper.ExamineeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CandidateAnsweringServiceTest {

    @Autowired
    private CandidateAnsweringService candidateAnsweringService;

    @Autowired
    private ExamineeMapper examineeMapper;

    @Autowired
    private ExamAnswerSessionMapper examAnswerSessionMapper;

    @Autowired
    private ExamResultMapper examResultMapper;

    @Autowired
    private ExamResultItemMapper examResultItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAnswerSessionAndSanitizeQuestionConfig() {
        Long planId = insertExamPlanForExaminee(201L, LocalDateTime.now().minusMinutes(15), LocalDateTime.now().plusMinutes(30));

        var response = candidateAnsweringService.loadAnswerSession(planId, confirmedContext(1L));

        assertThat(response.getPlanId()).isEqualTo(planId);
        assertThat(response.getSessionStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getTotalQuestionCount()).isEqualTo(2);
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions().getFirst().getAnswerConfig().has("correctOption")).isFalse();
        assertThat(response.getRemainingSeconds()).isPositive();
        assertThat(response.getDeadlineAt()).isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(30));
    }

    @Test
    void shouldAutoSubmitSessionWhenSavingAfterDeadline() throws Exception {
        Long planId = insertExamPlanForExaminee(202L, LocalDateTime.now().minusMinutes(15), LocalDateTime.now().plusMinutes(30));
        candidateAnsweringService.loadAnswerSession(planId, confirmedContext(1L));

        jdbcTemplate.update(
                "update exam_answer_session set deadline_at = ?, status = ? where exam_plan_id = ? and examinee_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(5)),
                "IN_PROGRESS",
                planId,
                1L
        );

        CandidateSaveAnswerRequest request = new CandidateSaveAnswerRequest();
        request.setAnswerContent(objectMapper.readTree("""
                {
                  "selectedOption": "A"
                }
                """));

        assertThatThrownBy(() -> candidateAnsweringService.saveAnswer(planId, 1L, request, confirmedContext(1L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("答题时间已结束");

        ExamAnswerSession session = loadSession(planId, 1L);
        assertThat(session.getStatus()).isEqualTo("AUTO_SUBMITTED");
        assertThat(session.getSubmittedAt()).isNotNull();
    }

    @Test
    void shouldKeepManualSubmissionIdempotent() throws Exception {
        Long planId = insertExamPlanForExaminee(203L, LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusMinutes(40));
        candidateAnsweringService.loadAnswerSession(planId, confirmedContext(1L));

        CandidateSaveAnswerRequest request = new CandidateSaveAnswerRequest();
        request.setAnswerContent(objectMapper.readTree("""
                {
                  "selectedOption": "A"
                }
                """));
        candidateAnsweringService.saveAnswer(planId, 1L, request, confirmedContext(1L));

        var firstSubmission = candidateAnsweringService.submitExam(planId, confirmedContext(1L));
        var secondSubmission = candidateAnsweringService.submitExam(planId, confirmedContext(1L));

        assertThat(firstSubmission.getSessionStatus()).isEqualTo("SUBMITTED");
        assertThat(firstSubmission.getSubmissionMethod()).isEqualTo("MANUAL");
        assertThat(firstSubmission.getAnsweredCount()).isEqualTo(1);
        assertThat(firstSubmission.getSubmittedAt()).isEqualTo(secondSubmission.getSubmittedAt());
        assertThat(secondSubmission.getSessionStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    void shouldGenerateScoreResultAfterManualSubmission() throws Exception {
        Long planId = insertExamPlanForExaminee(204L, LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusMinutes(40));
        candidateAnsweringService.loadAnswerSession(planId, confirmedContext(1L));

        CandidateSaveAnswerRequest singleChoice = new CandidateSaveAnswerRequest();
        singleChoice.setAnswerContent(objectMapper.readTree("""
                {
                  "selectedOption": "A"
                }
                """));
        candidateAnsweringService.saveAnswer(planId, 1L, singleChoice, confirmedContext(1L));

        CandidateSaveAnswerRequest textAnswer = new CandidateSaveAnswerRequest();
        textAnswer.setAnswerContent(objectMapper.readTree("""
                {
                  "textAnswer": "Java Virtual Machine"
                }
                """));
        candidateAnsweringService.saveAnswer(planId, 2L, textAnswer, confirmedContext(1L));

        candidateAnsweringService.submitExam(planId, confirmedContext(1L));

        ExamResult result = loadResult(planId, 1L);
        assertThat(result).isNotNull();
        assertThat(result.getScoreStatus()).isEqualTo("PUBLISHED");
        assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("11.00"));
        assertThat(result.getObjectiveScore()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.getSubjectiveScore()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(result.getAnsweredCount()).isEqualTo(2);
        assertThat(result.getUnansweredCount()).isEqualTo(0);
        assertThat(examResultItemMapper.selectCount(new LambdaQueryWrapper<cn.jack.exam.entity.ExamResultItem>()
                .eq(cn.jack.exam.entity.ExamResultItem::getResultId, result.getId()))).isEqualTo(2);
    }

    @Test
    void shouldGenerateScoreResultWhenSessionAutoSubmittedAfterDeadline() throws Exception {
        Long planId = insertExamPlanForExaminee(205L, LocalDateTime.now().minusMinutes(15), LocalDateTime.now().plusMinutes(30));
        candidateAnsweringService.loadAnswerSession(planId, confirmedContext(1L));

        CandidateSaveAnswerRequest request = new CandidateSaveAnswerRequest();
        request.setAnswerContent(objectMapper.readTree("""
                {
                  "selectedOption": "A"
                }
                """));
        candidateAnsweringService.saveAnswer(planId, 1L, request, confirmedContext(1L));

        jdbcTemplate.update(
                "update exam_answer_session set deadline_at = ?, status = ? where exam_plan_id = ? and examinee_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(5)),
                "IN_PROGRESS",
                planId,
                1L
        );

        CandidateSaveAnswerRequest expiredRequest = new CandidateSaveAnswerRequest();
        expiredRequest.setAnswerContent(objectMapper.readTree("""
                {
                  "textAnswer": "Java Virtual Machine"
                }
                """));

        assertThatThrownBy(() -> candidateAnsweringService.saveAnswer(planId, 2L, expiredRequest, confirmedContext(1L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("答题时间已结束");

        ExamAnswerSession session = loadSession(planId, 1L);
        assertThat(session.getStatus()).isEqualTo("AUTO_SUBMITTED");
        ExamResult result = loadResult(planId, 1L);
        assertThat(result).isNotNull();
        assertThat(result.getScoreStatus()).isEqualTo("PUBLISHED");
        assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.getAnsweredCount()).isEqualTo(1);
        assertThat(result.getUnansweredCount()).isEqualTo(1);
    }

    private CandidateUserContext confirmedContext(Long examineeId) {
        return CandidateUserContext.builder()
                .examinee(examineeMapper.selectById(examineeId))
                .profileConfirmed(true)
                .token("service-test-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private Long insertExamPlanForExaminee(Long planId, LocalDateTime startTime, LocalDateTime endTime) {
        jdbcTemplate.update(
                """
                insert into exam_plan (id, name, paper_id, start_time, end_time, status, remark, deleted, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                planId,
                "服务层在线答题场次-" + planId,
                1L,
                Timestamp.valueOf(startTime),
                Timestamp.valueOf(endTime),
                "PUBLISHED",
                "service-test",
                0
        );
        jdbcTemplate.update(
                "insert into exam_plan_examinee (exam_plan_id, examinee_id, created_at) values (?, ?, current_timestamp)",
                planId,
                1L
        );
        return planId;
    }

    private ExamAnswerSession loadSession(Long planId, Long examineeId) {
        return examAnswerSessionMapper.selectOne(new LambdaQueryWrapper<ExamAnswerSession>()
                .eq(ExamAnswerSession::getExamPlanId, planId)
                .eq(ExamAnswerSession::getExamineeId, examineeId)
                .last("limit 1"));
    }

    private ExamResult loadResult(Long planId, Long examineeId) {
        return examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, planId)
                .eq(ExamResult::getExamineeId, examineeId)
                .last("limit 1"));
    }
}
