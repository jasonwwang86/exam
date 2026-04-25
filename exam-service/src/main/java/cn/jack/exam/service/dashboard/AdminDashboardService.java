package cn.jack.exam.service.dashboard;

import cn.jack.exam.dto.admin.AdminDashboardSummaryResponse;
import cn.jack.exam.entity.ExamPlan;
import cn.jack.exam.entity.Examinee;
import cn.jack.exam.entity.Paper;
import cn.jack.exam.entity.Question;
import cn.jack.exam.mapper.ExamPlanMapper;
import cn.jack.exam.mapper.ExamineeMapper;
import cn.jack.exam.mapper.PaperMapper;
import cn.jack.exam.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final ExamineeMapper examineeMapper;
    private final QuestionMapper questionMapper;
    private final PaperMapper paperMapper;
    private final ExamPlanMapper examPlanMapper;

    public AdminDashboardSummaryResponse getSummary() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.toLocalDate().plusMonths(1).atStartOfDay();

        AdminDashboardSummaryResponse response = new AdminDashboardSummaryResponse();
        response.setMonthlyNewExamineeCount(countCurrentMonthExaminees(monthStart, nextMonthStart));
        response.setMonthlyNewQuestionCount(countCurrentMonthQuestions(monthStart, nextMonthStart));
        response.setMonthlyNewPaperCount(countCurrentMonthPapers(monthStart, nextMonthStart));
        response.setMonthlyActiveExamPlanCount(countCurrentMonthExamPlans(monthStart, nextMonthStart));
        return response;
    }

    private long countCurrentMonthExaminees(LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        return examineeMapper.selectCount(new LambdaQueryWrapper<Examinee>()
                .eq(Examinee::getDeleted, 0)
                .ge(Examinee::getCreatedAt, monthStart)
                .lt(Examinee::getCreatedAt, nextMonthStart));
    }

    private long countCurrentMonthQuestions(LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        return questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getDeleted, 0)
                .ge(Question::getCreatedAt, monthStart)
                .lt(Question::getCreatedAt, nextMonthStart));
    }

    private long countCurrentMonthPapers(LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        return paperMapper.selectCount(new LambdaQueryWrapper<Paper>()
                .eq(Paper::getDeleted, 0)
                .ge(Paper::getCreatedAt, monthStart)
                .lt(Paper::getCreatedAt, nextMonthStart));
    }

    private long countCurrentMonthExamPlans(LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        return examPlanMapper.selectCount(new LambdaQueryWrapper<ExamPlan>()
                .eq(ExamPlan::getDeleted, 0)
                .ge(ExamPlan::getStartTime, monthStart)
                .lt(ExamPlan::getStartTime, nextMonthStart));
    }
}
