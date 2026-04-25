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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnDashboardSummaryForAuthorizedAdmin() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyNewExamineeCount").value(3))
                .andExpect(jsonPath("$.monthlyNewQuestionCount").value(3))
                .andExpect(jsonPath("$.monthlyNewPaperCount").value(2))
                .andExpect(jsonPath("$.monthlyActiveExamPlanCount").value(0));
    }

    @Test
    void shouldRejectDashboardSummaryWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void shouldRejectDashboardSummaryWithoutPermission() throws Exception {
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/dashboard/summary")
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
