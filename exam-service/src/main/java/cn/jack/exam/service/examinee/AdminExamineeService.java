package cn.jack.exam.service.examinee;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.examinee.CreateExamineeRequest;
import cn.jack.exam.dto.examinee.ExamineeListItemResponse;
import cn.jack.exam.dto.examinee.ExamineePageResponse;
import cn.jack.exam.dto.examinee.ImportExamineeFailureResponse;
import cn.jack.exam.dto.examinee.ImportExamineeResultResponse;
import cn.jack.exam.dto.examinee.UpdateExamineeRequest;
import cn.jack.exam.dto.examinee.UpdateExamineeStatusRequest;
import cn.jack.exam.entity.Examinee;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.ExamineeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExamineeService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final ExamineeMapper examineeMapper;

    public ExamineePageResponse list(String keyword, String status, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safePage - 1) * safePageSize;
        List<ExamineeListItemResponse> records = examineeMapper.findPage(keyword, status, offset, safePageSize);
        long total = examineeMapper.countPage(keyword, status);
        return ExamineePageResponse.builder()
                .page(safePage)
                .pageSize(safePageSize)
                .total(total)
                .records(records)
                .build();
    }

    public ExamineeListItemResponse create(CreateExamineeRequest request) {
        validateUnique(null, request.getExamineeNo(), request.getIdCardNo());

        Examinee examinee = new Examinee();
        LocalDateTime now = LocalDateTime.now();
        examinee.setExamineeNo(request.getExamineeNo());
        examinee.setName(request.getName());
        examinee.setGender(request.getGender());
        examinee.setIdCardNo(request.getIdCardNo());
        examinee.setPhone(request.getPhone());
        examinee.setEmail(request.getEmail());
        examinee.setStatus(request.getStatus());
        examinee.setRemark(request.getRemark());
        examinee.setDeleted(0);
        examinee.setCreatedAt(now);
        examinee.setUpdatedAt(now);
        examineeMapper.insert(examinee);

        log.info("traceNo={} event=examinee_created examineeNo={} examineeName={} phone=*** idCardNo=***",
                TraceContext.getTraceNo(),
                examinee.getExamineeNo(),
                examinee.getName());

        return toResponse(examinee);
    }

    public ExamineeListItemResponse update(Long id, UpdateExamineeRequest request) {
        Examinee examinee = requireActive(id);
        validateUnique(id, examinee.getExamineeNo(), request.getIdCardNo());

        examinee.setName(request.getName());
        examinee.setGender(request.getGender());
        examinee.setIdCardNo(request.getIdCardNo());
        examinee.setPhone(request.getPhone());
        examinee.setEmail(request.getEmail());
        examinee.setStatus(request.getStatus());
        examinee.setRemark(request.getRemark());
        examinee.setUpdatedAt(LocalDateTime.now());
        examineeMapper.updateById(examinee);

        log.info("traceNo={} event=examinee_updated examineeNo={} examineeName={} phone=*** idCardNo=***",
                TraceContext.getTraceNo(),
                examinee.getExamineeNo(),
                examinee.getName());

        return toResponse(examinee);
    }

    public void delete(Long id) {
        Examinee examinee = requireActive(id);
        examinee.setDeleted(1);
        examinee.setUpdatedAt(LocalDateTime.now());
        examineeMapper.updateById(examinee);
        log.info("traceNo={} event=examinee_deleted examineeNo={} examineeName={}",
                TraceContext.getTraceNo(),
                examinee.getExamineeNo(),
                examinee.getName());
    }

    public ExamineeListItemResponse updateStatus(Long id, UpdateExamineeStatusRequest request) {
        Examinee examinee = requireActive(id);
        examinee.setStatus(request.getStatus());
        examinee.setUpdatedAt(LocalDateTime.now());
        examineeMapper.updateById(examinee);
        log.info("traceNo={} event=examinee_status_changed examineeNo={} status={}",
                TraceContext.getTraceNo(),
                examinee.getExamineeNo(),
                request.getStatus());
        return toResponse(examinee);
    }

    public byte[] export(String keyword, String status) {
        List<Examinee> examinees = examineeMapper.findForExport(keyword, status);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("考生");
            Row header = sheet.createRow(0);
            writeCell(header, 0, "考生编号");
            writeCell(header, 1, "姓名");
            writeCell(header, 2, "性别");
            writeCell(header, 3, "身份证号");
            writeCell(header, 4, "手机号");
            writeCell(header, 5, "邮箱");
            writeCell(header, 6, "状态");
            writeCell(header, 7, "备注");

            int rowIndex = 1;
            for (Examinee examinee : examinees) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, examinee.getExamineeNo());
                writeCell(row, 1, examinee.getName());
                writeCell(row, 2, examinee.getGender());
                writeCell(row, 3, examinee.getIdCardNo());
                writeCell(row, 4, examinee.getPhone());
                writeCell(row, 5, examinee.getEmail());
                writeCell(row, 6, examinee.getStatus());
                writeCell(row, 7, examinee.getRemark());
            }

            workbook.write(outputStream);
            log.info("traceNo={} event=examinee_exported count={}", TraceContext.getTraceNo(), examinees.size());
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BadRequestException("考生导出失败");
        }
    }

    public ImportExamineeResultResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("导入文件不能为空");
        }

        int successCount = 0;
        List<ImportExamineeFailureResponse> failures = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                try {
                    CreateExamineeRequest request = new CreateExamineeRequest();
                    request.setExamineeNo(readCell(row, 0));
                    request.setName(readCell(row, 1));
                    request.setGender(readCell(row, 2));
                    request.setIdCardNo(readCell(row, 3));
                    request.setPhone(readCell(row, 4));
                    request.setEmail(readCell(row, 5));
                    request.setStatus(readCell(row, 6));
                    request.setRemark(readCell(row, 7));
                    validateImportRow(request);
                    create(request);
                    successCount++;
                } catch (RuntimeException exception) {
                    failures.add(ImportExamineeFailureResponse.builder()
                            .rowNumber(rowIndex + 1)
                            .message(exception.getMessage())
                            .build());
                }
            }
        } catch (IOException exception) {
            throw new BadRequestException("导入文件格式不正确");
        }

        log.info("traceNo={} event=examinee_imported successCount={} failureCount={}",
                TraceContext.getTraceNo(),
                successCount,
                failures.size());

        return ImportExamineeResultResponse.builder()
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .build();
    }

    private void validateImportRow(CreateExamineeRequest request) {
        if (isBlank(request.getExamineeNo())) {
            throw new BadRequestException("考生编号不能为空");
        }
        if (isBlank(request.getName())) {
            throw new BadRequestException("姓名不能为空");
        }
        if (!"MALE".equals(request.getGender()) && !"FEMALE".equals(request.getGender())) {
            throw new BadRequestException("性别取值不合法");
        }
        if (isBlank(request.getIdCardNo())) {
            throw new BadRequestException("身份证号不能为空");
        }
        if (isBlank(request.getPhone())) {
            throw new BadRequestException("手机号不能为空");
        }
        if (!STATUS_ENABLED.equals(request.getStatus()) && !STATUS_DISABLED.equals(request.getStatus())) {
            throw new BadRequestException("状态取值不合法");
        }
    }

    private void validateUnique(Long currentId, String examineeNo, String idCardNo) {
        Examinee byExamineeNo = examineeMapper.selectOne(new LambdaQueryWrapper<Examinee>()
                .eq(Examinee::getExamineeNo, examineeNo)
                .eq(Examinee::getDeleted, 0)
                .last("limit 1"));
        if (byExamineeNo != null && !byExamineeNo.getId().equals(currentId)) {
            throw new BadRequestException("考生编号已存在");
        }

        Examinee byIdCardNo = examineeMapper.selectOne(new LambdaQueryWrapper<Examinee>()
                .eq(Examinee::getIdCardNo, idCardNo)
                .eq(Examinee::getDeleted, 0)
                .last("limit 1"));
        if (byIdCardNo != null && !byIdCardNo.getId().equals(currentId)) {
            throw new BadRequestException("身份证号已存在");
        }
    }

    private Examinee requireActive(Long id) {
        Examinee examinee = examineeMapper.selectById(id);
        if (examinee == null || examinee.getDeleted() == null || examinee.getDeleted() != 0) {
            throw new BadRequestException("考生不存在");
        }
        return examinee;
    }

    private ExamineeListItemResponse toResponse(Examinee examinee) {
        return ExamineeListItemResponse.builder()
                .id(examinee.getId())
                .examineeNo(examinee.getExamineeNo())
                .name(examinee.getName())
                .gender(examinee.getGender())
                .idCardNo(examinee.getIdCardNo())
                .phone(examinee.getPhone())
                .email(examinee.getEmail())
                .status(examinee.getStatus())
                .remark(examinee.getRemark())
                .updatedAt(examinee.getUpdatedAt())
                .build();
    }

    private void writeCell(Row row, int columnIndex, String value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
    }

    private String readCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : cell.toString().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
