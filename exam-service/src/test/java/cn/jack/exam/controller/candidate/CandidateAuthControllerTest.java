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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CandidateAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExamPlanMapper examPlanMapper;

    @Autowired
    private ExamPlanExamineeMapper examPlanExamineeMapper;

    @Test
    void shouldLoginCandidateWithValidCredentials() throws Exception {
        mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026001",
                                  "idCardNo": "110101199001010011"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.profileConfirmed").value(false))
                .andExpect(jsonPath("$.profile.examineeId").value(1))
                .andExpect(jsonPath("$.profile.examineeNo").value("EX2026001"))
                .andExpect(jsonPath("$.profile.name").value("张三"))
                .andExpect(jsonPath("$.profile.maskedIdCardNo").value("110101********0011"));
    }

    @Test
    void shouldRejectCandidateLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026001",
                                  "idCardNo": "wrong-id-card"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("考生编号或身份证号错误"));
    }

    @Test
    void shouldRejectDisabledCandidateLogin() throws Exception {
        mockMvc.perform(post("/api/candidate/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026002",
                                  "idCardNo": "110101199202020022"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("考生编号或身份证号错误"));
    }

    @Test
    void shouldRequireCandidateTokenForProtectedCandidateApi() throws Exception {
        mockMvc.perform(get("/api/candidate/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("考生登录已失效或不存在"));
    }

    @Test
    void shouldRejectAdminTokenOnCandidateApi() throws Exception {
        String adminToken = loginAdminAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/candidate/profile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("考生登录已失效或不存在"));
    }

    @Test
    void shouldReturnCandidateProfileBeforeConfirmation() throws Exception {
        String candidateToken = loginCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(get("/api/candidate/profile")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examineeId").value(1))
                .andExpect(jsonPath("$.examineeNo").value("EX2026001"))
                .andExpect(jsonPath("$.name").value("张三"))
                .andExpect(jsonPath("$.maskedIdCardNo").value("110101********0011"))
                .andExpect(jsonPath("$.profileConfirmed").value(false));
    }

    @Test
    void shouldRejectExamListBeforeProfileConfirmation() throws Exception {
        String candidateToken = loginCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(get("/api/candidate/exams")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("请先确认身份信息"));
    }

    @Test
    void shouldConfirmProfileAndReturnRefreshedToken() throws Exception {
        String candidateToken = loginCandidateAndExtractToken("EX2026001", "110101199001010011");

        MvcResult result = mockMvc.perform(post("/api/candidate/profile/confirm")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.profileConfirmed").value(true))
                .andExpect(jsonPath("$.profile.examineeNo").value("EX2026001"))
                .andReturn();

        String refreshedToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get("/api/candidate/exams")
                        .header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].planId").value(1))
                .andExpect(jsonPath("$[0].name").value("Java 基础考试-上午场"))
                .andExpect(jsonPath("$[0].paperName").value("Java 基础试卷"))
                .andExpect(jsonPath("$[0].durationMinutes").value(120))
                .andExpect(jsonPath("$[0].displayStatus").value("待开始"));
    }

    @Test
    void shouldFilterUnavailableExamPlansFromCandidateExamList() throws Exception {
        insertUnavailableExamPlan(3L, "历史已结束考试", "PUBLISHED",
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 1, 11, 0),
                1L);
        insertUnavailableExamPlan(4L, "已关闭考试", "CLOSED",
                LocalDateTime.of(2026, 5, 10, 9, 0),
                LocalDateTime.of(2026, 5, 10, 11, 0),
                1L);
        insertUnavailableExamPlan(5L, "草稿考试", "DRAFT",
                LocalDateTime.of(2026, 5, 12, 9, 0),
                LocalDateTime.of(2026, 5, 12, 11, 0),
                1L);

        String candidateToken = confirmCandidateAndExtractToken("EX2026001", "110101199001010011");

        mockMvc.perform(get("/api/candidate/exams")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].planId").value(1))
                .andExpect(jsonPath("$[0].name").value("Java 基础考试-上午场"));
    }

    @Test
    void shouldReuseProvidedTraceNoOnCandidateApis() throws Exception {
        String traceNo = "723e4567e89b12d3a456426614174000";

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
    }

    private String loginAdminAndExtractToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
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

    private void insertUnavailableExamPlan(Long planId,
                                           String name,
                                           String status,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           Long examineeId) {
        ExamPlan plan = new ExamPlan();
        plan.setId(planId);
        plan.setName(name);
        plan.setPaperId(1L);
        plan.setStartTime(startTime);
        plan.setEndTime(endTime);
        plan.setStatus(status);
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
    }
}
