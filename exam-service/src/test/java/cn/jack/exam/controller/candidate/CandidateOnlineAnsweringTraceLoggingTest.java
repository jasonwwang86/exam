package cn.jack.exam.controller.candidate;

import cn.jack.exam.entity.ExamPlan;
import cn.jack.exam.entity.ExamPlanExaminee;
import cn.jack.exam.mapper.ExamPlanExamineeMapper;
import cn.jack.exam.mapper.ExamPlanMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class CandidateOnlineAnsweringTraceLoggingTest {

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
    void shouldLogAnswerSessionLoadingAndAnswerSavingWithoutLeakingAnswerContent(CapturedOutput output) throws Exception {
        Long planId = insertExamPlanForExaminee(
                200L,
                "日志校验在线答题场次",
                LocalDateTime.now().minusMinutes(15),
                LocalDateTime.now().plusMinutes(30),
                1L);
        String traceNo = "a23e4567e89b12d3a456426614174000";
        String token = confirmCandidateAndExtractToken();

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        mockMvc.perform(put("/api/candidate/exams/{planId}/questions/{paperQuestionId}/answer", planId, 1L)
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answerContent": {
                                    "selectedOption": "A"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("candidate_answer_session_loaded");
        assertThat(output.getOut()).contains("candidate_answer_saved");
        assertThat(output.getOut()).doesNotContain("selectedOption");
        assertThat(output.getOut()).doesNotContain(token);
        assertThat(output.getOut()).doesNotContain("Authorization: Bearer " + token);
    }

    @Test
    void shouldLogTimeoutRejectionWithoutLeakingSensitiveFields(CapturedOutput output) throws Exception {
        Long planId = insertExamPlanForExaminee(
                201L,
                "日志校验超时拒绝场次",
                LocalDateTime.now().minusMinutes(15),
                LocalDateTime.now().plusMinutes(30),
                1L);
        String traceNo = "b23e4567e89b12d3a456426614174000";
        String token = confirmCandidateAndExtractToken();

        mockMvc.perform(put("/api/candidate/exams/{planId}/answer-session", planId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update exam_answer_session set deadline_at = ?, status = ? where exam_plan_id = ? and examinee_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(5)),
                "TIME_EXPIRED",
                planId,
                1L);

        mockMvc.perform(put("/api/candidate/exams/{planId}/questions/{paperQuestionId}/answer", planId, 1L)
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answerContent": {
                                    "selectedOption": "B"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("candidate_answer_save_rejected");
        assertThat(output.getOut()).contains("TIME_EXPIRED");
        assertThat(output.getOut()).doesNotContain("selectedOption");
        assertThat(output.getOut()).doesNotContain(token);
    }

    private String confirmCandidateAndExtractToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026001",
                                  "idCardNo": "110101199001010011"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("token").asText();

        MvcResult confirmResult = mockMvc.perform(post("/api/candidate/profile/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode confirmJson = objectMapper.readTree(confirmResult.getResponse().getContentAsString());
        return confirmJson.get("token").asText();
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
