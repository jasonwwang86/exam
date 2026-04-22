package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.question.QuestionTypeResponse;
import cn.jack.exam.dto.question.SaveQuestionTypeRequest;
import cn.jack.exam.service.question.AdminQuestionTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/question-types")
@RequiredArgsConstructor
public class AdminQuestionTypeController {

    private final AdminQuestionTypeService adminQuestionTypeService;

    @GetMapping
    @RequirePermission("question-type:read")
    public List<QuestionTypeResponse> list() {
        return adminQuestionTypeService.list();
    }

    @PostMapping
    @RequirePermission("question-type:create")
    public QuestionTypeResponse create(@Valid @RequestBody SaveQuestionTypeRequest request) {
        return adminQuestionTypeService.create(request);
    }

    @PutMapping("/{id}")
    @RequirePermission("question-type:update")
    public QuestionTypeResponse update(@PathVariable Long id, @Valid @RequestBody SaveQuestionTypeRequest request) {
        return adminQuestionTypeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("question-type:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        adminQuestionTypeService.delete(id);
        return Map.of("success", true);
    }
}
