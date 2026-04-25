package cn.jack.exam.controller.candidate;

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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class CandidateTraceLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReuseTraceNoAndMaskSensitiveFieldsForCandidateLogin(CapturedOutput output) throws Exception {
        String traceNo = "823e4567e89b12d3a456426614174000";

        mockMvc.perform(post("/api/candidate/auth/login")
                        .header("TraceNo", traceNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026001",
                                  "idCardNo": "110101199001010011"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("candidate_login_success");
        assertThat(output.getOut()).doesNotContain("110101199001010011");
        assertThat(output.getOut()).contains("\"token\":\"***\"");
    }

    @Test
    void shouldLogConfirmationAndExamListWithoutLeakingToken(CapturedOutput output) throws Exception {
        String traceNo = "923e4567e89b12d3a456426614174000";
        String token = loginCandidateAndExtractToken();

        MvcResult confirmResult = mockMvc.perform(post("/api/candidate/profile/confirm")
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo))
                .andReturn();

        String refreshedToken = objectMapper.readTree(confirmResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/candidate/exams")
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("candidate_profile_confirmed");
        assertThat(output.getOut()).contains("candidate_exam_listed");
        assertThat(output.getOut()).doesNotContain(refreshedToken);
        assertThat(output.getOut()).doesNotContain("Authorization: Bearer " + refreshedToken);
    }

    @Test
    void shouldLogScoreReportQueryWithoutLeakingAnswerSummary(CapturedOutput output) throws Exception {
        String traceNo = "a23e4567e89b12d3a456426614174999";
        String token = confirmCandidateAndExtractToken();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                insert into exam_answer_session (
                    exam_plan_id, examinee_id, paper_id, started_at, deadline_at, status, last_saved_at, submitted_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L,
                1L,
                1L,
                Timestamp.valueOf(now.minusMinutes(20)),
                Timestamp.valueOf(now.plusMinutes(100)),
                "SUBMITTED",
                Timestamp.valueOf(now.minusMinutes(10)),
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(20)),
                Timestamp.valueOf(now.minusMinutes(5))
        );
        Long sessionId = jdbcTemplate.queryForObject(
                "select id from exam_answer_session where exam_plan_id = ? and examinee_id = ?",
                Long.class,
                1L,
                1L
        );
        jdbcTemplate.update(
                """
                insert into exam_result (
                    exam_plan_id, examinee_id, session_id, paper_id, score_status, total_score, objective_score, subjective_score,
                    answered_count, unanswered_count, submitted_at, generated_at, published_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                1L,
                1L,
                sessionId,
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
        Long resultId = jdbcTemplate.queryForObject(
                "select id from exam_result where exam_plan_id = ? and examinee_id = ?",
                Long.class,
                1L,
                1L
        );
        jdbcTemplate.update(
                """
                insert into exam_result_item (
                    result_id, paper_question_id, question_id, question_no, question_stem_snapshot, question_type_name_snapshot,
                    item_score, awarded_score, answer_status, answer_summary, judge_status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                resultId,
                1L,
                1L,
                1,
                "Java 的入口方法是什么？",
                "单选题",
                new BigDecimal("5.00"),
                new BigDecimal("5.00"),
                "ANSWERED",
                "选择 A",
                "CORRECT",
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(2))
        );

        mockMvc.perform(get("/api/candidate/exams/{planId}/score-report", 1L)
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("candidate_score_report_queried");
        assertThat(output.getOut()).doesNotContain("选择 A");
        assertThat(output.getOut()).doesNotContain(token);
        assertThat(output.getOut()).doesNotContain("Authorization: Bearer " + token);
    }

    @Test
    void shouldMaskTextAnswerSummaryInScoreReportResponseLog(CapturedOutput output) throws Exception {
        String traceNo = "b23e4567e89b12d3a456426614174999";
        String token = confirmCandidateAndExtractToken();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                "insert into paper (id, name, description, duration_minutes, total_score, remark, deleted, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                901L,
                "短卷",
                "短卷",
                30,
                new BigDecimal("6.00"),
                "短卷",
                0,
                Timestamp.valueOf(now.minusMinutes(20)),
                Timestamp.valueOf(now.minusMinutes(20))
        );
        jdbcTemplate.update(
                """
                insert into exam_plan (id, name, paper_id, start_time, end_time, status, remark, deleted, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                902L,
                "短场次",
                901L,
                Timestamp.valueOf(now.minusHours(1)),
                Timestamp.valueOf(now.plusHours(1)),
                "PUBLISHED",
                "短场次",
                0,
                Timestamp.valueOf(now.minusHours(1)),
                Timestamp.valueOf(now.minusHours(1))
        );
        jdbcTemplate.update(
                "insert into exam_plan_examinee (exam_plan_id, examinee_id, created_at) values (?, ?, ?)",
                902L,
                1L,
                Timestamp.valueOf(now.minusHours(1))
        );
        jdbcTemplate.update(
                """
                insert into exam_answer_session (
                    exam_plan_id, examinee_id, paper_id, started_at, deadline_at, status, last_saved_at, submitted_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                902L,
                1L,
                901L,
                Timestamp.valueOf(now.minusMinutes(20)),
                Timestamp.valueOf(now.plusMinutes(10)),
                "SUBMITTED",
                Timestamp.valueOf(now.minusMinutes(10)),
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(20)),
                Timestamp.valueOf(now.minusMinutes(5))
        );
        Long sessionId = jdbcTemplate.queryForObject(
                "select id from exam_answer_session where exam_plan_id = ? and examinee_id = ?",
                Long.class,
                902L,
                1L
        );
        jdbcTemplate.update(
                """
                insert into exam_result (
                    exam_plan_id, examinee_id, session_id, paper_id, score_status, total_score, objective_score, subjective_score,
                    answered_count, unanswered_count, submitted_at, generated_at, published_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                902L,
                1L,
                sessionId,
                901L,
                "PUBLISHED",
                new BigDecimal("6.00"),
                BigDecimal.ZERO,
                new BigDecimal("6.00"),
                1,
                0,
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(1)),
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(1))
        );
        Long resultId = jdbcTemplate.queryForObject(
                "select id from exam_result where exam_plan_id = ? and examinee_id = ?",
                Long.class,
                902L,
                1L
        );
        jdbcTemplate.update(
                """
                insert into exam_result_item (
                    result_id, paper_question_id, question_id, question_no, question_stem_snapshot, question_type_name_snapshot,
                    item_score, awarded_score, answer_status, answer_summary, judge_status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                resultId,
                901L,
                901L,
                1,
                "什么是JVM",
                "简答题",
                new BigDecimal("6.00"),
                new BigDecimal("6.00"),
                "ANSWERED",
                "Java Virtual Machine",
                "CORRECT",
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(2))
        );

        mockMvc.perform(get("/api/candidate/exams/{planId}/score-report", 902L)
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("candidate_score_report_queried");
        assertThat(output.getOut()).doesNotContain("Java Virtual Machine");
        assertThat(output.getOut()).doesNotContain(token);
        assertThat(output.getOut()).doesNotContain("Authorization: Bearer " + token);
    }

    private String loginCandidateAndExtractToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026001",
                                  "idCardNo": "110101199001010011"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    private String confirmCandidateAndExtractToken() throws Exception {
        String token = loginCandidateAndExtractToken();
        MvcResult result = mockMvc.perform(post("/api/candidate/profile/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }
}
