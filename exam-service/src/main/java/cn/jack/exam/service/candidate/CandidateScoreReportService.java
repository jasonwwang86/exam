package cn.jack.exam.service.candidate;

import cn.jack.exam.config.CandidateUserContext;
import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.candidate.CandidateScoreReportItemResponse;
import cn.jack.exam.dto.candidate.CandidateScoreReportResponse;
import cn.jack.exam.entity.ExamAnswerRecord;
import cn.jack.exam.entity.ExamAnswerSession;
import cn.jack.exam.entity.ExamPlan;
import cn.jack.exam.entity.ExamResult;
import cn.jack.exam.entity.ExamResultItem;
import cn.jack.exam.entity.Paper;
import cn.jack.exam.entity.PaperQuestion;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.mapper.ExamAnswerRecordMapper;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamPlanExamineeMapper;
import cn.jack.exam.mapper.ExamPlanMapper;
import cn.jack.exam.mapper.ExamResultItemMapper;
import cn.jack.exam.mapper.ExamResultMapper;
import cn.jack.exam.mapper.PaperQuestionMapper;
import cn.jack.exam.mapper.PaperMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateScoreReportService {

    private final ExamPlanMapper examPlanMapper;
    private final ExamPlanExamineeMapper examPlanExamineeMapper;
    private final ExamAnswerSessionMapper examAnswerSessionMapper;
    private final ExamAnswerRecordMapper examAnswerRecordMapper;
    private final ExamResultMapper examResultMapper;
    private final ExamResultItemMapper examResultItemMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final PaperMapper paperMapper;
    private final ObjectMapper objectMapper;

    public CandidateScoreReportResponse getScoreReport(Long planId, CandidateUserContext context) {
        if (!context.isProfileConfirmed()) {
            throw new ForbiddenException("请先确认身份信息");
        }

        ExamPlan plan = examPlanMapper.selectById(planId);
        if (plan == null || plan.getDeleted() == null || plan.getDeleted() != 0 || !"PUBLISHED".equals(plan.getStatus())) {
            throw new BadRequestException("考试不存在");
        }

        long relationCount = examPlanExamineeMapper.selectCount(new LambdaQueryWrapper<cn.jack.exam.entity.ExamPlanExaminee>()
                .eq(cn.jack.exam.entity.ExamPlanExaminee::getExamPlanId, planId)
                .eq(cn.jack.exam.entity.ExamPlanExaminee::getExamineeId, context.getExaminee().getId()));
        if (relationCount == 0) {
            throw new ForbiddenException("无权查看该考试成绩");
        }

        ExamAnswerSession session = examAnswerSessionMapper.selectOne(new LambdaQueryWrapper<ExamAnswerSession>()
                .eq(ExamAnswerSession::getExamPlanId, planId)
                .eq(ExamAnswerSession::getExamineeId, context.getExaminee().getId())
                .last("limit 1"));
        if (session == null || !isFinalSubmitted(session)) {
            throw new BadRequestException("成绩未生成");
        }

        ExamResult result = examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, planId)
                .eq(ExamResult::getExamineeId, context.getExaminee().getId())
                .last("limit 1"));
        if (result == null) {
            throw new BadRequestException("成绩未生成");
        }

        Paper paper = paperMapper.selectById(result.getPaperId());
        if (paper == null || paper.getDeleted() == null || paper.getDeleted() != 0) {
            throw new BadRequestException("试卷不存在");
        }

        List<ExamResultItem> resultItems = examResultItemMapper.selectList(new LambdaQueryWrapper<ExamResultItem>()
                .eq(ExamResultItem::getResultId, result.getId())
                .orderByAsc(ExamResultItem::getQuestionNo));
        Map<Long, PaperQuestion> questionMap = loadPaperQuestionMap(result.getPaperId());
        Map<Long, ExamAnswerRecord> answerRecordMap = loadAnswerRecordMap(session.getId());

        List<CandidateScoreReportItemResponse> items = resultItems
                .stream()
                .map(item -> CandidateScoreReportItemResponse.builder()
                        .paperQuestionId(item.getPaperQuestionId())
                        .questionId(item.getQuestionId())
                        .questionNo(item.getQuestionNo())
                        .questionStem(item.getQuestionStemSnapshot())
                        .questionTypeName(item.getQuestionTypeNameSnapshot())
                        .answerMode(resolveAnswerMode(item, questionMap.get(item.getPaperQuestionId())))
                        .answerConfig(sanitizeAnswerConfig(questionMap.get(item.getPaperQuestionId())))
                        .itemScore(item.getItemScore())
                        .awardedScore(item.getAwardedScore())
                        .answerStatus(item.getAnswerStatus())
                        .answerSummary(item.getAnswerSummary())
                        .savedAnswer(parseJson(answerRecordMap.get(item.getPaperQuestionId()) == null ? null : answerRecordMap.get(item.getPaperQuestionId()).getAnswerContent()))
                        .judgeStatus(item.getJudgeStatus())
                        .build())
                .toList();

        log.info("traceNo={} event=candidate_score_report_queried candidateId={} planId={} resultId={} scoreStatus={} itemCount={}",
                TraceContext.getTraceNo(),
                context.getExaminee().getId(),
                planId,
                result.getId(),
                result.getScoreStatus(),
                items.size());

        return CandidateScoreReportResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .paperName(paper.getName())
                .durationMinutes(paper.getDurationMinutes())
                .remark(plan.getRemark())
                .scoreStatus(result.getScoreStatus())
                .totalScore(result.getTotalScore())
                .objectiveScore(result.getObjectiveScore())
                .subjectiveScore(result.getSubjectiveScore())
                .answeredCount(result.getAnsweredCount())
                .unansweredCount(result.getUnansweredCount())
                .submittedAt(result.getSubmittedAt())
                .generatedAt(result.getGeneratedAt())
                .publishedAt(result.getPublishedAt())
                .submissionMethod(resolveSubmissionMethod(session.getStatus()))
                .items(items)
                .build();
    }

    private boolean isFinalSubmitted(ExamAnswerSession session) {
        return "SUBMITTED".equals(session.getStatus()) || "AUTO_SUBMITTED".equals(session.getStatus());
    }

    private Map<Long, PaperQuestion> loadPaperQuestionMap(Long paperId) {
        Map<Long, PaperQuestion> questionMap = new HashMap<>();
        paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, paperId)
                        .eq(PaperQuestion::getDeleted, 0))
                .forEach(question -> questionMap.put(question.getId(), question));
        return questionMap;
    }

    private Map<Long, ExamAnswerRecord> loadAnswerRecordMap(Long sessionId) {
        Map<Long, ExamAnswerRecord> answerRecordMap = new HashMap<>();
        examAnswerRecordMapper.selectList(new LambdaQueryWrapper<ExamAnswerRecord>()
                        .eq(ExamAnswerRecord::getSessionId, sessionId))
                .forEach(record -> answerRecordMap.put(record.getPaperQuestionId(), record));
        return answerRecordMap;
    }

    private String resolveAnswerMode(ExamResultItem item, PaperQuestion question) {
        String questionTypeName = item.getQuestionTypeNameSnapshot();
        if ("单选题".equals(questionTypeName)) {
            return "SINGLE_CHOICE";
        }
        if ("多选题".equals(questionTypeName)) {
            return "MULTIPLE_CHOICE";
        }
        if ("判断题".equals(questionTypeName)) {
            return "TRUE_FALSE";
        }
        if ("简答题".equals(questionTypeName)) {
            return "TEXT";
        }
        JsonNode answerConfig = parseJson(question == null ? null : question.getAnswerConfigSnapshot());
        if (answerConfig != null && answerConfig.has("correctOption")) {
            return "SINGLE_CHOICE";
        }
        if (answerConfig != null && answerConfig.has("correctOptions")) {
            return "MULTIPLE_CHOICE";
        }
        if (answerConfig != null && answerConfig.has("correctAnswer")) {
            return "TRUE_FALSE";
        }
        return "TEXT";
    }

    private JsonNode sanitizeAnswerConfig(PaperQuestion question) {
        JsonNode answerConfig = parseJson(question == null ? null : question.getAnswerConfigSnapshot());
        if (answerConfig == null || !answerConfig.isObject()) {
            return answerConfig;
        }
        JsonNode copied = answerConfig.deepCopy();
        copied = copied.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) copied)
                .remove(List.of("correctOption", "correctOptions", "correctAnswer", "acceptedAnswers"));
        return copied;
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new BadRequestException("成绩详情数据异常");
        }
    }

    private String resolveSubmissionMethod(String sessionStatus) {
        if ("AUTO_SUBMITTED".equals(sessionStatus)) {
            return "AUTO";
        }
        return "MANUAL";
    }
}
