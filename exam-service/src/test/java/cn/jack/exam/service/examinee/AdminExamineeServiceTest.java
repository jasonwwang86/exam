package cn.jack.exam.service.examinee;

import cn.jack.exam.dto.examinee.CreateExamineeRequest;
import cn.jack.exam.exception.BadRequestException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminExamineeServiceTest {

    @Autowired
    private AdminExamineeService adminExamineeService;

    @Test
    void shouldCreateExamineeWhenBusinessKeysAreUnique() {
        CreateExamineeRequest request = buildCreateRequest("EX2026999", "110101199909090099");

        var response = adminExamineeService.create(request);

        assertThat(response.getExamineeNo()).isEqualTo("EX2026999");
        assertThat(response.getName()).isEqualTo("测试考生");
        assertThat(response.getStatus()).isEqualTo("ENABLED");
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void shouldRejectCreateWhenExamineeNoAlreadyExists() {
        CreateExamineeRequest request = buildCreateRequest("EX2026001", "110101199909090099");

        assertThatThrownBy(() -> adminExamineeService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("考生编号已存在");
    }

    @Test
    void shouldImportWorkbookAndCollectRowFailures() throws Exception {
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("考生");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("考生编号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("性别");
            header.createCell(3).setCellValue("身份证号");
            header.createCell(4).setCellValue("手机号");
            header.createCell(5).setCellValue("邮箱");
            header.createCell(6).setCellValue("状态");
            header.createCell(7).setCellValue("备注");

            var validRow = sheet.createRow(1);
            validRow.createCell(0).setCellValue("EX2026888");
            validRow.createCell(1).setCellValue("导入成功考生");
            validRow.createCell(2).setCellValue("MALE");
            validRow.createCell(3).setCellValue("110101199808080088");
            validRow.createCell(4).setCellValue("13800000888");
            validRow.createCell(5).setCellValue("import-success@example.com");
            validRow.createCell(6).setCellValue("ENABLED");
            validRow.createCell(7).setCellValue("ok");

            var invalidRow = sheet.createRow(2);
            invalidRow.createCell(0).setCellValue("EX2026889");
            invalidRow.createCell(1).setCellValue("导入失败考生");
            invalidRow.createCell(2).setCellValue("MALE");
            invalidRow.createCell(3).setCellValue("110101199808080089");
            invalidRow.createCell(4).setCellValue("13800000889");
            invalidRow.createCell(5).setCellValue("import-fail@example.com");
            invalidRow.createCell(6).setCellValue("UNKNOWN");
            invalidRow.createCell(7).setCellValue("invalid");

            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "examinees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );

        var result = adminExamineeService.importFile(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getFailures()).singleElement()
                .satisfies(failure -> {
                    assertThat(failure.getRowNumber()).isEqualTo(3);
                    assertThat(failure.getMessage()).isEqualTo("状态取值不合法");
                });
    }

    @Test
    void shouldExportFilteredExamineesAsWorkbook() throws Exception {
        byte[] exportBytes = adminExamineeService.export("张三", null);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
            var sheet = workbook.getSheetAt(0);

            assertThat(sheet.getSheetName()).isEqualTo("考生");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("考生编号");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("EX2026001");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("张三");
        }
    }

    private CreateExamineeRequest buildCreateRequest(String examineeNo, String idCardNo) {
        CreateExamineeRequest request = new CreateExamineeRequest();
        request.setExamineeNo(examineeNo);
        request.setName("测试考生");
        request.setGender("MALE");
        request.setIdCardNo(idCardNo);
        request.setPhone("13800138000");
        request.setEmail("service-test@example.com");
        request.setStatus("ENABLED");
        request.setRemark("service-test");
        return request;
    }
}
