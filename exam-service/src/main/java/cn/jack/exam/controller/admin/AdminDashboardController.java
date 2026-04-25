package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import cn.jack.exam.dto.admin.AdminDashboardSummaryResponse;
import cn.jack.exam.service.dashboard.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    @RequirePermission("dashboard:read")
    public AdminDashboardSummaryResponse summary() {
        return adminDashboardService.getSummary();
    }
}
