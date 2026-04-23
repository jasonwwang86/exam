package cn.jack.exam.controller.admin;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminExamPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListExamPlansByKeywordAndStatus() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");
        String traceNo = "623e4567e89b12d3a456426614174000";

        mockMvc.perform(get("/api/admin/exam-plans")
                        .header("Authorization", "Bearer " + token)
                        .header("TraceNo", traceNo)
                        .param("keyword", "Java")
                        .param("status", "PUBLISHED")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo))
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].name").value("Java 基础考试-上午场"))
                .andExpect(jsonPath("$.records[0].paperName").value("Java 基础试卷"))
                .andExpect(jsonPath("$.records[0].effectiveExamineeCount").value(2))
                .andExpect(jsonPath("$.records[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldGetExamPlanDetail() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/exam-plans/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java 基础考试-上午场"))
                .andExpect(jsonPath("$.paperId").value(1))
                .andExpect(jsonPath("$.paperName").value("Java 基础试卷"))
                .andExpect(jsonPath("$.paperDurationMinutes").value(120))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.effectiveExamineeCount").value(2))
                .andExpect(jsonPath("$.invalidExamineeCount").value(0));
    }

    @Test
    void shouldCreateExamPlanInDraftStatus() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/exam-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Spring 考试-下午场",
                                  "paperId": 1,
                                  "startTime": "2026-05-01T14:00:00",
                                  "endTime": "2026-05-01T17:00:00",
                                  "remark": "首次安排"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring 考试-下午场"))
                .andExpect(jsonPath("$.paperId").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.effectiveExamineeCount").value(0));
    }

    @Test
    void shouldRejectExamPlanWithTimeWindowShorterThanPaperDuration() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/exam-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "非法时间窗口考试",
                                  "paperId": 1,
                                  "startTime": "2026-05-01T09:00:00",
                                  "endTime": "2026-05-01T10:00:00",
                                  "remark": "窗口不足"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("考试时间窗口不能短于试卷时长"));
    }

    @Test
    void shouldUpdateExamPlanBaseInfo() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/exam-plans/2")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "待安排考试-已更新",
                                  "paperId": 2,
                                  "startTime": "2026-05-02T09:30:00",
                                  "endTime": "2026-05-02T11:30:00",
                                  "remark": "补充安排"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("待安排考试-已更新"))
                .andExpect(jsonPath("$.paperId").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldReplaceExamPlanExaminees() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/exam-plans/2/examinees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeIds": [1, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(2))
                .andExpect(jsonPath("$.effectiveExamineeCount").value(2));

        mockMvc.perform(get("/api/admin/exam-plans/2/examinees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].examineeNo").value("EX2026001"))
                .andExpect(jsonPath("$[1].examineeNo").value("EX2026003"));
    }

    @Test
    void shouldPublishConfiguredExamPlan() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(patch("/api/admin/exam-plans/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void shouldRejectPublishingExamPlanWithoutExaminees() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(patch("/api/admin/exam-plans/2/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("考试范围不能为空"));
    }

    @Test
    void shouldRejectExamPlanApiWithoutPermission() throws Exception {
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/exam-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
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
}
