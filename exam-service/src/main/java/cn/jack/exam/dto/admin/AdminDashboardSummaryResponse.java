package cn.jack.exam.dto.admin;

import lombok.Data;

@Data
public class AdminDashboardSummaryResponse {

    private long monthlyNewExamineeCount;

    private long monthlyNewQuestionCount;

    private long monthlyNewPaperCount;

    private long monthlyActiveExamPlanCount;
}
