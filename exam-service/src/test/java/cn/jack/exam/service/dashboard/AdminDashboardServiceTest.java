package cn.jack.exam.service.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AdminDashboardServiceTest {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Test
    void shouldSummarizeCurrentMonthDashboardMetrics() {
        var summary = adminDashboardService.getSummary();

        assertThat(summary.getMonthlyNewExamineeCount()).isEqualTo(3L);
        assertThat(summary.getMonthlyNewQuestionCount()).isEqualTo(3L);
        assertThat(summary.getMonthlyNewPaperCount()).isEqualTo(2L);
        assertThat(summary.getMonthlyActiveExamPlanCount()).isZero();
    }
}
