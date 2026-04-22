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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListPapersByKeyword() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/papers")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "Java")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].name").value("Java 基础试卷"))
                .andExpect(jsonPath("$.records[0].questionCount").value(2))
                .andExpect(jsonPath("$.records[0].totalScore").value(11.0))
                .andExpect(jsonPath("$.records[0].durationMinutes").value(120))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldGetPaperDetail() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/papers/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java 基础试卷"))
                .andExpect(jsonPath("$.description").value("覆盖 Java 基础知识"))
                .andExpect(jsonPath("$.durationMinutes").value(120))
                .andExpect(jsonPath("$.totalScore").value(11.0))
                .andExpect(jsonPath("$.questionCount").value(2))
                .andExpect(jsonPath("$.remark").value("首套试卷"));
    }

    @Test
    void shouldCreateEmptyPaper() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/papers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Spring 专题试卷",
                                  "description": "覆盖 Spring 核心概念",
                                  "durationMinutes": 100,
                                  "remark": "待组卷"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring 专题试卷"))
                .andExpect(jsonPath("$.durationMinutes").value(100))
                .andExpect(jsonPath("$.totalScore").value(0.0))
                .andExpect(jsonPath("$.questionCount").value(0));
    }

    @Test
    void shouldRejectInvalidPaperDuration() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/papers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "非法时长试卷",
                                  "description": "非法时长",
                                  "durationMinutes": 0,
                                  "remark": "测试"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("考试时长必须为正整数"));
    }

    @Test
    void shouldUpdatePaperBaseInfo() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/papers/2")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "空白练习卷-已更新",
                                  "description": "已补充说明",
                                  "durationMinutes": 95,
                                  "remark": "更新后"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("空白练习卷-已更新"))
                .andExpect(jsonPath("$.durationMinutes").value(95))
                .andExpect(jsonPath("$.remark").value("更新后"));
    }

    @Test
    void shouldDeletePaper() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(delete("/api/admin/papers/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/papers")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)));
    }

    @Test
    void shouldListPaperQuestions() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/papers/1/questions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].questionId").value(1))
                .andExpect(jsonPath("$[0].questionStemSnapshot").value("Java 的入口方法是什么？"))
                .andExpect(jsonPath("$[0].itemScore").value(5.0))
                .andExpect(jsonPath("$[0].displayOrder").value(1));
    }

    @Test
    void shouldAddPaperQuestionsAndRefreshTotalScore() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/papers/2/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionIds": [1, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].questionId").value(1))
                .andExpect(jsonPath("$[1].questionId").value(3));

        mockMvc.perform(get("/api/admin/papers/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCount").value(2))
                .andExpect(jsonPath("$.totalScore").value(7.0));
    }

    @Test
    void shouldRejectAddingDuplicateQuestionToPaper() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/papers/1/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("题目已存在于当前试卷中"));
    }

    @Test
    void shouldUpdatePaperQuestionAndRecalculateTotalScore() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/papers/1/questions/2")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemScore": 10,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.itemScore").value(10.0))
                .andExpect(jsonPath("$.displayOrder").value(1));

        mockMvc.perform(get("/api/admin/papers/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(15.0));
    }

    @Test
    void shouldDeletePaperQuestionAndRefreshTotalScore() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(delete("/api/admin/papers/1/questions/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/papers/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCount").value(1))
                .andExpect(jsonPath("$.totalScore").value(5.0));
    }

    @Test
    void shouldRejectPaperApiWithoutPermission() throws Exception {
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/papers")
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
