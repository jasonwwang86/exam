package cn.jack.exam.controller.candidate;

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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CandidatePortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectCandidateLoginWhenExamineeNoIsBlank() throws Exception {
        mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "",
                                  "idCardNo": "110101199001010011"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("考生编号不能为空"));
    }

    @Test
    void shouldReturnCandidateProfileAfterLogin() throws Exception {
        String token = loginCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(get("/api/candidate/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examineeNo").value("EX2026001"))
                .andExpect(jsonPath("$.profileConfirmed").value(false))
                .andExpect(jsonPath("$.message").value("请先确认身份信息后查看可参加考试"));
    }

    @Test
    void shouldReturnAvailableExamsAfterProfileConfirmation() throws Exception {
        String token = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(get("/api/candidate/exams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].planId").value(1))
                .andExpect(jsonPath("$[0].name").value("Java 基础考试-上午场"));
    }

    @Test
    void shouldRejectScoreReportWhenResultNotReady() throws Exception {
        String token = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");
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

        mockMvc.perform(get("/api/candidate/exams/{planId}/score-report", 1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("成绩未生成"));
    }

    @Test
    void shouldReturnScoreReportWhenResultReady() throws Exception {
        String token = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");
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
        jdbcTemplate.update(
                """
                insert into exam_result_item (
                    result_id, paper_question_id, question_id, question_no, question_stem_snapshot, question_type_name_snapshot,
                    item_score, awarded_score, answer_status, answer_summary, judge_status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                resultId,
                2L,
                2L,
                2,
                "请写出 JVM 的英文全称。",
                "简答题",
                new BigDecimal("6.00"),
                new BigDecimal("6.00"),
                "ANSWERED",
                "Java Virtual Machine",
                "CORRECT",
                Timestamp.valueOf(now.minusMinutes(2)),
                Timestamp.valueOf(now.minusMinutes(2))
        );
        jdbcTemplate.update(
                """
                insert into exam_answer_record (
                    session_id, paper_question_id, question_id, answer_content, answer_status, last_saved_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                1L,
                1L,
                "{\"selectedOption\":\"A\"}",
                "ANSWERED",
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(5))
        );
        jdbcTemplate.update(
                """
                insert into exam_answer_record (
                    session_id, paper_question_id, question_id, answer_content, answer_status, last_saved_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                2L,
                2L,
                "{\"textAnswer\":\"Java Virtual Machine\"}",
                "ANSWERED",
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(5))
        );

        mockMvc.perform(get("/api/candidate/exams/{planId}/score-report", 1L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.name").value("Java 基础考试-上午场"))
                .andExpect(jsonPath("$.paperName").value("Java 基础试卷"))
                .andExpect(jsonPath("$.scoreStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.totalScore").value(92.5))
                .andExpect(jsonPath("$.answeredCount").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].questionNo").value(1))
                .andExpect(jsonPath("$.items[0].awardedScore").value(5.0))
                .andExpect(jsonPath("$.items[0].answerSummary").value("选择 A"))
                .andExpect(jsonPath("$.items[0].answerMode").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.items[0].answerConfig.options", hasSize(4)))
                .andExpect(jsonPath("$.items[0].answerConfig.correctOption").doesNotExist())
                .andExpect(jsonPath("$.items[0].savedAnswer.selectedOption").value("A"))
                .andExpect(jsonPath("$.items[1].answerMode").value("TEXT"))
                .andExpect(jsonPath("$.items[1].savedAnswer.textAnswer").value("Java Virtual Machine"));
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
        String token = loginCandidateAndExtractToken(examineeNo, idCardNo);
        MvcResult result = mockMvc.perform(post("/api/candidate/profile/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }
}
