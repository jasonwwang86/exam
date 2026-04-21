package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.examinee.CreateExamineeRequest;
import cn.jack.exam.dto.examinee.ExamineeListItemResponse;
import cn.jack.exam.dto.examinee.ExamineePageResponse;
import cn.jack.exam.dto.examinee.ImportExamineeResultResponse;
import cn.jack.exam.dto.examinee.UpdateExamineeRequest;
import cn.jack.exam.dto.examinee.UpdateExamineeStatusRequest;
import cn.jack.exam.service.examinee.AdminExamineeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/examinees")
@RequiredArgsConstructor
public class AdminExamineeController {

    private final AdminExamineeService adminExamineeService;

    @GetMapping
    @RequirePermission("examinee:read")
    public ExamineePageResponse list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long pageSize) {
        return adminExamineeService.list(keyword, status, page, pageSize);
    }

    @PostMapping
    @RequirePermission("examinee:create")
    public ExamineeListItemResponse create(@Valid @RequestBody CreateExamineeRequest request) {
        return adminExamineeService.create(request);
    }

    @PutMapping("/{id}")
    @RequirePermission("examinee:update")
    public ExamineeListItemResponse update(@PathVariable Long id, @Valid @RequestBody UpdateExamineeRequest request) {
        return adminExamineeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("examinee:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        adminExamineeService.delete(id);
        return Map.of("success", true);
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("examinee:status")
    public ExamineeListItemResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateExamineeStatusRequest request) {
        return adminExamineeService.updateStatus(id, request);
    }

    @GetMapping("/export")
    @RequirePermission("examinee:export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=examinees.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(adminExamineeService.export(keyword, status));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("examinee:import")
    public ImportExamineeResultResponse importFile(@RequestPart("file") MultipartFile file) {
        return adminExamineeService.importFile(file);
    }
}
