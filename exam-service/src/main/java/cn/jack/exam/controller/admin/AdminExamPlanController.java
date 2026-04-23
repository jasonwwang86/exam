package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.examplan.ExamPlanDetailResponse;
import cn.jack.exam.dto.examplan.ExamPlanExamineeResponse;
import cn.jack.exam.dto.examplan.ExamPlanPageResponse;
import cn.jack.exam.dto.examplan.SaveExamPlanRequest;
import cn.jack.exam.dto.examplan.UpdateExamPlanExamineesRequest;
import cn.jack.exam.dto.examplan.UpdateExamPlanExamineesResponse;
import cn.jack.exam.dto.examplan.UpdateExamPlanStatusRequest;
import cn.jack.exam.service.examplan.AdminExamPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/exam-plans")
@RequiredArgsConstructor
public class AdminExamPlanController {

    private final AdminExamPlanService adminExamPlanService;

    @GetMapping
    @RequirePermission("exam-plan:read")
    public ExamPlanPageResponse list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long pageSize) {
        return adminExamPlanService.list(keyword, status, page, pageSize);
    }

    @GetMapping("/{id}")
    @RequirePermission("exam-plan:read")
    public ExamPlanDetailResponse get(@PathVariable Long id) {
        return adminExamPlanService.get(id);
    }

    @PostMapping
    @RequirePermission("exam-plan:create")
    public ExamPlanDetailResponse create(@Valid @RequestBody SaveExamPlanRequest request) {
        return adminExamPlanService.create(request);
    }

    @PutMapping("/{id}")
    @RequirePermission("exam-plan:update")
    public ExamPlanDetailResponse update(@PathVariable Long id, @Valid @RequestBody SaveExamPlanRequest request) {
        return adminExamPlanService.update(id, request);
    }

    @PutMapping("/{id}/examinees")
    @RequirePermission("exam-plan:range")
    public UpdateExamPlanExamineesResponse replaceExaminees(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateExamPlanExamineesRequest request) {
        return adminExamPlanService.replaceExaminees(id, request);
    }

    @GetMapping("/{id}/examinees")
    @RequirePermission("exam-plan:read")
    public List<ExamPlanExamineeResponse> listExaminees(@PathVariable Long id) {
        return adminExamPlanService.listExaminees(id);
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("exam-plan:status")
    public ExamPlanDetailResponse updateStatus(@PathVariable Long id,
                                               @Valid @RequestBody UpdateExamPlanStatusRequest request) {
        return adminExamPlanService.updateStatus(id, request);
    }
}
