package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.paper.CreatePaperQuestionsRequest;
import cn.jack.exam.dto.paper.PaperQuestionResponse;
import cn.jack.exam.dto.paper.UpdatePaperQuestionRequest;
import cn.jack.exam.service.paper.AdminPaperQuestionService;
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
@RequestMapping("/api/admin/papers/{paperId}/questions")
@RequiredArgsConstructor
public class AdminPaperQuestionController {

    private final AdminPaperQuestionService adminPaperQuestionService;

    @GetMapping
    @RequirePermission("paper-question:read")
    public List<PaperQuestionResponse> list(@PathVariable Long paperId) {
        return adminPaperQuestionService.list(paperId);
    }

    @PostMapping
    @RequirePermission("paper-question:create")
    public List<PaperQuestionResponse> create(@PathVariable Long paperId,
                                              @Valid @RequestBody CreatePaperQuestionsRequest request) {
        return adminPaperQuestionService.create(paperId, request);
    }

    @PutMapping("/{paperQuestionId}")
    @RequirePermission("paper-question:update")
    public PaperQuestionResponse update(@PathVariable Long paperId,
                                        @PathVariable Long paperQuestionId,
                                        @Valid @RequestBody UpdatePaperQuestionRequest request) {
        return adminPaperQuestionService.update(paperId, paperQuestionId, request);
    }

    @DeleteMapping("/{paperQuestionId}")
    @RequirePermission("paper-question:delete")
    public Map<String, Object> delete(@PathVariable Long paperId, @PathVariable Long paperQuestionId) {
        adminPaperQuestionService.delete(paperId, paperQuestionId);
        return Map.of("success", true);
    }
}
