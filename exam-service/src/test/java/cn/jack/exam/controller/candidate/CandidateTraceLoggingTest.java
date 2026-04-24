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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
}
