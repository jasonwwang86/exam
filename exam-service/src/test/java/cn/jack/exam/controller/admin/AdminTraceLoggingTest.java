package cn.jack.exam.controller.admin;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AdminTraceLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReuseProvidedTraceNoInResponseHeaderAndLogs(CapturedOutput output) throws Exception {
        String traceNo = "123e4567e89b12d3a456426614174000";

        mockMvc.perform(post("/api/admin/auth/login")
                        .header("TraceNo", traceNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
    }

    @Test
    void shouldGenerateTraceNoWhenHeaderIsMissing(CapturedOutput output) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("TraceNo"))
                .andReturn();

        String traceNo = result.getResponse().getHeader("TraceNo");
        assertThat(traceNo).isNotBlank();
        assertThat(traceNo).matches("[0-9a-f]{32}");
        assertThat(output.getOut()).contains(traceNo);
    }

    @Test
    void shouldReplaceInvalidProvidedTraceNoWithGeneratedNoHyphenUuid(CapturedOutput output) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("TraceNo", "trace-login-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("TraceNo"))
                .andReturn();

        String traceNo = result.getResponse().getHeader("TraceNo");
        assertThat(traceNo).matches("[0-9a-f]{32}");
        assertThat(traceNo).isNotEqualTo("trace-login-001");
        assertThat(output.getOut()).contains(traceNo);
    }

    @Test
    void shouldMaskSensitiveFieldsInRequestResponseLogs(CapturedOutput output) throws Exception {
        String traceNo = "223e4567e89b12d3a456426614174000";

        mockMvc.perform(post("/api/admin/auth/login")
                        .header("TraceNo", traceNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).doesNotContain("Admin@123456");
        assertThat(output.getOut()).contains("\"token\":\"***\"");
    }

    @Test
    void shouldLogPermissionDeniedWithTraceNo(CapturedOutput output) throws Exception {
        String traceNo = "323e4567e89b12d3a456426614174000";
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("TraceNo", traceNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(header().string("TraceNo", traceNo));

        assertThat(output.getOut()).contains(traceNo);
        assertThat(output.getOut()).contains("permission_denied");
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

        String response = result.getResponse().getContentAsString();
        int start = response.indexOf("\"token\":\"") + 9;
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }
}
