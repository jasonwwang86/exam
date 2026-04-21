package cn.jack.exam.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminExamineeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListExamineesByKeywordAndStatus() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/examinees")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "张")
                        .param("status", "ENABLED")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].examineeNo").value("EX2026001"))
                .andExpect(jsonPath("$.records[0].name").value("张三"))
                .andExpect(jsonPath("$.records[0].status").value("ENABLED"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldCreateExaminee() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/examinees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026099",
                                  "name": "赵六",
                                  "gender": "FEMALE",
                                  "idCardNo": "310101199901011234",
                                  "phone": "13900000099",
                                  "email": "zhaoliu@example.com",
                                  "status": "ENABLED",
                                  "remark": "新导入考生"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examineeNo").value("EX2026099"))
                .andExpect(jsonPath("$.name").value("赵六"))
                .andExpect(jsonPath("$.status").value("ENABLED"));
    }

    @Test
    void shouldRejectDuplicateExamineeNo() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/examinees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examineeNo": "EX2026001",
                                  "name": "重复考生",
                                  "gender": "MALE",
                                  "idCardNo": "310101199901011235",
                                  "phone": "13900000111",
                                  "email": "duplicate@example.com",
                                  "status": "ENABLED",
                                  "remark": "重复编号"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("考生编号已存在"));
    }

    @Test
    void shouldUpdateExaminee() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(put("/api/admin/examinees/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "张三-已更新",
                                  "gender": "MALE",
                                  "idCardNo": "110101199001010011",
                                  "phone": "13800000009",
                                  "email": "zhangsan-updated@example.com",
                                  "status": "ENABLED",
                                  "remark": "已更新"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("张三-已更新"))
                .andExpect(jsonPath("$.phone").value("13800000009"))
                .andExpect(jsonPath("$.email").value("zhangsan-updated@example.com"));
    }

    @Test
    void shouldDeleteExaminee() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(delete("/api/admin/examinees/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/examinees")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(2)));
    }

    @Test
    void shouldUpdateExamineeStatus() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(patch("/api/admin/examinees/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void shouldExportFilteredExaminees() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/examinees/export")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "ENABLED"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=examinees.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void shouldImportExamineesAndReturnResultSummary() throws Exception {
        String token = loginAndExtractToken("admin", "Admin@123456");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "examinees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createImportWorkbookBytes()
        );

        mockMvc.perform(multipart("/api/admin/examinees/import")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    @Test
    void shouldRejectExamineeApiWithoutPermission() throws Exception {
        String token = loginAndExtractToken("limited-admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/examinees")
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

    private byte[] createImportWorkbookBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("考生");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("考生编号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("性别");
            header.createCell(3).setCellValue("身份证号");
            header.createCell(4).setCellValue("手机号");
            header.createCell(5).setCellValue("邮箱");
            header.createCell(6).setCellValue("状态");
            header.createCell(7).setCellValue("备注");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("EX2026100");
            row.createCell(1).setCellValue("导入考生");
            row.createCell(2).setCellValue("MALE");
            row.createCell(3).setCellValue("110101199404040044");
            row.createCell(4).setCellValue("13800000100");
            row.createCell(5).setCellValue("imported@example.com");
            row.createCell(6).setCellValue("ENABLED");
            row.createCell(7).setCellValue("批量导入");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
