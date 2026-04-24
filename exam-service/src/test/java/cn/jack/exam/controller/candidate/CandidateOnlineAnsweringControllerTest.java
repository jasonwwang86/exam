package cn.jack.exam.controller.candidate;

import cn.jack.exam.entity.ExamPlan;
import cn.jack.exam.entity.ExamPlanExaminee;
import cn.jack.exam.mapper.ExamPlanExamineeMapper;
import cn.jack.exam.mapper.ExamPlanMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CandidateOnlineAnsweringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExamPlanMapper examPlanMapper;

    @Autowired
    private ExamPlanExamineeMapper examPlanExamineeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateAnswerSessionForConfirmedCandidateWithinAnswerWindow() throws Exception {
        Long planId = insertExamPlanForExaminee(
                100L,
                "Java 在线答题场次",
                LocalDateTime.now().minusMinutes(15),
                LocalDateTime.now().plusMinutes(30),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.sessionStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isString())
                .andExpect(jsonPath("$.deadlineAt").isString())
                .andExpect(jsonPath("$.remainingSeconds", greaterThan(0)))
                .andExpect(jsonPath("$.questions", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].paperQuestionId").value(1))
                .andExpect(jsonPath("$.questions[0].questionNo").value(1))
                .andExpect(jsonPath("$.questions[0].stem").value("Java 的入口方法是什么？"))
                .andExpect(jsonPath("$.questions[0].answerStatus").value("UNANSWERED"))
                .andExpect(jsonPath("$.questions[0].answerConfig.options", hasSize(4)))
                .andExpect(jsonPath("$.questions[0].answerConfig.correctOption").doesNotExist());
    }

    @Test
    void shouldRestoreExistingAnswerSessionWithSavedAnswers() throws Exception {
        Long planId = insertExamPlanForExaminee(
                101L,
                "Java 在线答题恢复场次",
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusMinutes(45),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/candidate/exams/{planId}/questions/{paperQuestionId}/answer", planId, 1L)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answerContent": {
                                    "selectedOption": "A"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperQuestionId").value(1))
                .andExpect(jsonPath("$.answerStatus").value("ANSWERED"))
                .andExpect(jsonPath("$.answeredCount").value(1));

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.questions[0].answerStatus").value("ANSWERED"))
                .andExpect(jsonPath("$.questions[0].savedAnswer.selectedOption").value("A"));
    }

    @Test
    void shouldRejectAnswerSessionBeforeProfileConfirmation() throws Exception {
        Long planId = insertExamPlanForExaminee(
                102L,
                "待确认考生不可答题场次",
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusMinutes(30),
                1L);
        String candidateToken = loginCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("请先确认身份信息"));
    }

    @Test
    void shouldRejectAnswerSessionWhenExamHasNotStarted() throws Exception {
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", 1L)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前考试未到答题时间"));
    }

    @Test
    void shouldRejectAnswerSessionWhenExamHasEnded() throws Exception {
        Long planId = insertExamPlanForExaminee(
                103L,
                "已结束考试场次",
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusMinutes(5),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前考试已结束"));
    }

    @Test
    void shouldRejectSavingAnswerWhenSessionHasExpired() throws Exception {
        Long planId = insertExamPlanForExaminee(
                104L,
                "超时保存拦截场次",
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusMinutes(20),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update exam_answer_session set deadline_at = ?, status = ? where exam_plan_id = ? and examinee_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(5)),
                "TIME_EXPIRED",
                planId,
                1L);

        mockMvc.perform(put("/api/candidate/exams/{planId}/questions/{paperQuestionId}/answer", planId, 1L)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answerContent": {
                                    "selectedOption": "B"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("答题时间已结束"));
    }

    @Test
    void shouldSubmitExamManuallyAndKeepSubmissionIdempotent() throws Exception {
        Long planId = insertExamPlanForExaminee(
                105L,
                "主动交卷场次",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusMinutes(40),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/candidate/exams/{planId}/questions/{paperQuestionId}/answer", planId, 1L)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answerContent": {
                                    "selectedOption": "A"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult firstSubmission = mockMvc.perform(post("/api/candidate/exams/{planId}/submission", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.sessionStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.submissionMethod").value("MANUAL"))
                .andExpect(jsonPath("$.submittedAt").isString())
                .andExpect(jsonPath("$.answeredCount").value(1))
                .andExpect(jsonPath("$.totalQuestionCount").value(2))
                .andReturn();

        String firstSubmittedAt = objectMapper.readTree(firstSubmission.getResponse().getContentAsString())
                .get("submittedAt")
                .asText();

        mockMvc.perform(post("/api/candidate/exams/{planId}/submission", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.submissionMethod").value("MANUAL"))
                .andExpect(jsonPath("$.submittedAt").value(firstSubmittedAt));
    }

    @Test
    void shouldAutoSubmitExpiredSessionWhenReloadingAnswerSession() throws Exception {
        Long planId = insertExamPlanForExaminee(
                106L,
                "到时自动交卷场次",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusMinutes(40),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update exam_answer_session set deadline_at = ?, status = ? where exam_plan_id = ? and examinee_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(3)),
                "IN_PROGRESS",
                planId,
                1L);

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionStatus").value("AUTO_SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").isString());
    }

    @Test
    void shouldRejectSavingAnswerAfterSessionSubmitted() throws Exception {
        Long planId = insertExamPlanForExaminee(
                107L,
                "交卷后不可继续保存场次",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusMinutes(40),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update exam_answer_session set status = ? where exam_plan_id = ? and examinee_id = ?",
                "SUBMITTED",
                planId,
                1L);

        mockMvc.perform(put("/api/candidate/exams/{planId}/questions/{paperQuestionId}/answer", planId, 1L)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answerContent": {
                                    "selectedOption": "B"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("试卷已提交，不能重复作答"));
    }

    @Test
    void shouldExposeAnsweringSummaryOnCandidateExamList() throws Exception {
        Long planId = insertExamPlanForExaminee(
                108L,
                "考试列表在线答题摘要场次",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusMinutes(40),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/candidate/exams")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.planId == 108)].canEnterAnswering").value(true))
                .andExpect(jsonPath("$[?(@.planId == 108)].answeringStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[?(@.planId == 108)].remainingSeconds").isNotEmpty());
    }

    @Test
    void shouldExposeSubmittedSummaryOnCandidateExamList() throws Exception {
        Long planId = insertExamPlanForExaminee(
                109L,
                "考试列表已交卷摘要场次",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().plusMinutes(40),
                1L);
        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update exam_answer_session set status = ?, last_saved_at = ? where exam_plan_id = ? and examinee_id = ?",
                "SUBMITTED",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)),
                planId,
                1L);

        mockMvc.perform(get("/api/candidate/exams")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.planId == 109)].canEnterAnswering").value(false))
                .andExpect(jsonPath("$[?(@.planId == 109)].answeringStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$[?(@.planId == 109)].submittedAt").isNotEmpty());
    }

    private String loginCandidateAndExtractToken(String examineeNo, String idCardNo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "%s",
                                  "idCardNo": "%s"
                                }
                                """.formatted(examineeNo, idCardNo)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    private String confirmCandidateAndExtractToken(String examineeNo, String idCardNo) throws Exception {
        String candidateToken = loginCandidateAndExtractToken(examineeNo, idCardNo);
        MvcResult result = mockMvc.perform(post("/api/candidate/profile/confirm")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    private Long insertExamPlanForExaminee(Long planId,
                                           String name,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           Long examineeId) {
        ExamPlan plan = new ExamPlan();
        plan.setId(planId);
        plan.setName(name);
        plan.setPaperId(1L);
        plan.setStartTime(startTime);
        plan.setEndTime(endTime);
        plan.setStatus("PUBLISHED");
        plan.setRemark(name);
        plan.setDeleted(0);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        examPlanMapper.insert(plan);

        ExamPlanExaminee relation = new ExamPlanExaminee();
        relation.setId(planId);
        relation.setExamPlanId(planId);
        relation.setExamineeId(examineeId);
        relation.setCreatedAt(LocalDateTime.now());
        examPlanExamineeMapper.insert(relation);
        return planId;
    }
}
