package cn.jack.exam.controller.candidate;

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
