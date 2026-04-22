package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.question.QuestionDetailResponse;
import cn.jack.exam.dto.question.QuestionPageResponse;
import cn.jack.exam.dto.question.SaveQuestionRequest;
import cn.jack.exam.service.question.AdminQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    @GetMapping
    @RequirePermission("question:read")
    public QuestionPageResponse list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long questionTypeId,
                                     @RequestParam(required = false) String difficulty,
                                     @RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long pageSize) {
        return adminQuestionService.list(keyword, questionTypeId, difficulty, page, pageSize);
    }

    @GetMapping("/{id}")
    @RequirePermission("question:read")
    public QuestionDetailResponse get(@PathVariable Long id) {
        return adminQuestionService.get(id);
    }

    @PostMapping
    @RequirePermission("question:create")
    public QuestionDetailResponse create(@Valid @RequestBody SaveQuestionRequest request) {
        return adminQuestionService.create(request);
    }

    @PutMapping("/{id}")
    @RequirePermission("question:update")
    public QuestionDetailResponse update(@PathVariable Long id, @Valid @RequestBody SaveQuestionRequest request) {
        return adminQuestionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("question:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        adminQuestionService.delete(id);
        return Map.of("success", true);
    }
}
