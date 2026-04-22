package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.paper.PaperDetailResponse;
import cn.jack.exam.dto.paper.PaperPageResponse;
import cn.jack.exam.dto.paper.SavePaperRequest;
import cn.jack.exam.service.paper.AdminPaperService;
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
@RequestMapping("/api/admin/papers")
@RequiredArgsConstructor
public class AdminPaperController {

    private final AdminPaperService adminPaperService;

    @GetMapping
    @RequirePermission("paper:read")
    public PaperPageResponse list(@RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") long page,
                                  @RequestParam(defaultValue = "10") long pageSize) {
        return adminPaperService.list(keyword, page, pageSize);
    }

    @GetMapping("/{id}")
    @RequirePermission("paper:read")
    public PaperDetailResponse get(@PathVariable Long id) {
        return adminPaperService.get(id);
    }

    @PostMapping
    @RequirePermission("paper:create")
    public PaperDetailResponse create(@Valid @RequestBody SavePaperRequest request) {
        return adminPaperService.create(request);
    }

    @PutMapping("/{id}")
    @RequirePermission("paper:update")
    public PaperDetailResponse update(@PathVariable Long id, @Valid @RequestBody SavePaperRequest request) {
        return adminPaperService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("paper:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        adminPaperService.delete(id);
        return Map.of("success", true);
    }
}
