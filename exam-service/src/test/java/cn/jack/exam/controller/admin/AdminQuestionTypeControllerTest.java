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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminQuestionTypeControllerTest {

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
                .andExpect(jsonPath("$[0].name").value("单选题"));
    }

    @Test
    void shouldCreateQuestionType() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/question-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "材料题",
                                  "answerMode": "TEXT",
                                  "sort": 60,
                                  "remark": "新题型"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("材料题"))
                .andExpect(jsonPath("$.answerMode").value("TEXT"))
                .andExpect(jsonPath("$.sort").value(60));
    }

    @Test
    void shouldDeleteUnusedQuestionType() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");
        MvcResult created = mockMvc.perform(post("/api/admin/question-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "可删除题型",
                                  "answerMode": "TEXT",
                                  "sort": 61,
                                  "remark": "delete-test"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/admin/question-types/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRejectQuestionTypeApiWithoutPermission() throws Exception {
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/question-types")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Permission denied"));
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
