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
class AdminQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListQuestionTypes() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/question-types")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].name").value("单选题"))
                .andExpect(jsonPath("$[0].answerMode").value("SINGLE_CHOICE"));
    }

    @Test
    void shouldCreateQuestionType() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/question-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "填空题",
                                  "answerMode": "TEXT",
                                  "sort": 50,
                                  "remark": "文本填空"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("填空题"))
                .andExpect(jsonPath("$.answerMode").value("TEXT"))
                .andExpect(jsonPath("$.sort").value(50));
    }

    @Test
    void shouldUpdateQuestionType() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/question-types/4")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "问答题",
                                  "answerMode": "TEXT",
                                  "sort": 45,
                                  "remark": "人工评分"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("问答题"))
                .andExpect(jsonPath("$.answerMode").value("TEXT"))
                .andExpect(jsonPath("$.sort").value(45))
                .andExpect(jsonPath("$.remark").value("人工评分"));
    }

    @Test
    void shouldRejectDeletingReferencedQuestionType() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(delete("/api/admin/question-types/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("题型已被题目引用，无法删除"));
    }

    @Test
    void shouldListQuestionsByKeywordTypeAndDifficulty() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "入口")
                        .param("questionTypeId", "1")
                        .param("difficulty", "EASY")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].stem").value("Java 的入口方法是什么？"))
                .andExpect(jsonPath("$.records[0].questionTypeName").value("单选题"))
                .andExpect(jsonPath("$.records[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.records[0].score").value(5.0))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldGetQuestionDetail() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/questions/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.questionTypeId").value(1))
                .andExpect(jsonPath("$.questionTypeName").value("单选题"))
                .andExpect(jsonPath("$.answerMode").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.answerConfig.options", hasSize(4)))
                .andExpect(jsonPath("$.answerConfig.correctOption").value("A"));
    }

    @Test
    void shouldCreateQuestion() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stem": "HTTP 默认端口是什么？",
                                  "questionTypeId": 1,
                                  "difficulty": "EASY",
                                  "score": 3,
                                  "answerConfig": {
                                    "options": [
                                      {"key": "A", "content": "80"},
                                      {"key": "B", "content": "443"},
                                      {"key": "C", "content": "3306"},
                                      {"key": "D", "content": "8080"}
                                    ],
                                    "correctOption": "A"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stem").value("HTTP 默认端口是什么？"))
                .andExpect(jsonPath("$.questionTypeId").value(1))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.score").value(3.0))
                .andExpect(jsonPath("$.answerConfig.correctOption").value("A"));
    }

    @Test
    void shouldRejectQuestionWhenAnswerConfigDoesNotMatchTypeMode() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stem": "布尔题配置错误",
                                  "questionTypeId": 3,
                                  "difficulty": "MEDIUM",
                                  "score": 2,
                                  "answerConfig": {
                                    "options": [
                                      {"key": "A", "content": "正确"},
                                      {"key": "B", "content": "错误"}
                                    ],
                                    "correctOption": "A"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("答案配置与题型不匹配"));
    }

    @Test
    void shouldUpdateQuestion() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/questions/2")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stem": "JVM 全称是什么？",
                                  "questionTypeId": 4,
                                  "difficulty": "MEDIUM",
                                  "score": 8,
                                  "answerConfig": {
                                    "acceptedAnswers": ["Java Virtual Machine", "Java虚拟机"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stem").value("JVM 全称是什么？"))
                .andExpect(jsonPath("$.questionTypeId").value(4))
                .andExpect(jsonPath("$.answerMode").value("TEXT"))
                .andExpect(jsonPath("$.score").value(8.0));
    }

    @Test
    void shouldDeleteQuestion() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(delete("/api/admin/questions/3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(2)));
    }

    @Test
    void shouldRejectQuestionApiWithoutPermission() throws Exception {
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/questions")
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
