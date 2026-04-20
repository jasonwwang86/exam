package cn.jack.exam.controller.admin;

import cn.jack.exam.config.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @GetMapping("/summary")
    @RequirePermission("dashboard:read")
    public Map<String, Object> summary() {
        return Map.of("message", "ok");
    }
}
