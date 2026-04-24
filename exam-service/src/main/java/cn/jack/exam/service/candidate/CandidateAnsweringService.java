package cn.jack.exam.service.candidate;

import cn.jack.exam.config.CandidateUserContext;
import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.candidate.CandidateAnswerQuestionItemResponse;
import cn.jack.exam.dto.candidate.CandidateAnswerQuestionView;
import cn.jack.exam.dto.candidate.CandidateAnswerSessionResponse;
import cn.jack.exam.dto.candidate.CandidateSaveAnswerRequest;
import cn.jack.exam.dto.candidate.CandidateSaveAnswerResponse;
import cn.jack.exam.entity.ExamAnswerRecord;
import cn.jack.exam.entity.ExamAnswerSession;
import cn.jack.exam.entity.ExamPlan;
import cn.jack.exam.entity.ExamPlanExaminee;
import cn.jack.exam.entity.Paper;
import cn.jack.exam.entity.PaperQuestion;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.mapper.ExamAnswerRecordMapper;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamPlanExamineeMapper;
import cn.jack.exam.mapper.ExamPlanMapper;
import cn.jack.exam.mapper.PaperMapper;
import cn.jack.exam.mapper.PaperQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateAnsweringService {

    private final ExamPlanMapper examPlanMapper;
    private final ExamPlanExamineeMapper examPlanExamineeMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamAnswerSessionMapper examAnswerSessionMapper;
    private final ExamAnswerRecordMapper examAnswerRecordMapper;
    private final ObjectMapper objectMapper;

    public CandidateAnswerSessionResponse loadAnswerSession(Long planId, CandidateUserContext context) {
        ensureProfileConfirmed(context);

        LocalDateTime now = LocalDateTime.now();
        ExamPlan plan = requireCandidatePlan(planId, context.getExaminee().getId());
        if (now.isBefore(plan.getStartTime())) {
            throw new ForbiddenException("当前考试未到答题时间");
        }
        if (!now.isBefore(plan.getEndTime())) {
            throw new ForbiddenException("当前考试已结束");
        }

        Paper paper = requireActivePaper(plan.getPaperId());
        ExamAnswerSession session = findSession(planId, context.getExaminee().getId());
        if (session == null) {
            session = createSession(plan, paper, context.getExaminee().getId(), now);
        } else if (isExpired(session, now)) {
            session = markTimeExpired(session, now);
        }

        List<CandidateAnswerQuestionView> questionViews = paperQuestionMapper.findCandidateQuestionsByPaperId(paper.getId());
        Map<Long, ExamAnswerRecord> recordByQuestionId = loadRecordMap(session.getId());
        int answeredCount = countAnswered(session.getId());

        log.info("traceNo={} event=candidate_answer_session_loaded candidateId={} planId={} sessionId={} sessionStatus={} remainingSeconds={} questionCount={}",
                TraceContext.getTraceNo(),
                context.getExaminee().getId(),
                planId,
                session.getId(),
                session.getStatus(),
                remainingSeconds(session.getDeadlineAt(), now),
                questionViews.size());

        return CandidateAnswerSessionResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .paperName(paper.getName())
                .durationMinutes(paper.getDurationMinutes())
                .sessionStatus(session.getStatus())
                .startedAt(session.getStartedAt())
                .deadlineAt(session.getDeadlineAt())
                .remainingSeconds(remainingSeconds(session.getDeadlineAt(), now))
                .answeredCount(answeredCount)
                .totalQuestionCount(questionViews.size())
                .questions(questionViews.stream()
                        .map(question -> toQuestionItem(question, recordByQuestionId.get(question.getPaperQuestionId())))
                        .toList())
                .build();
    }

    public CandidateSaveAnswerResponse saveAnswer(Long planId,
                                                  Long paperQuestionId,
                                                  CandidateSaveAnswerRequest request,
                                                  CandidateUserContext context) {
        ensureProfileConfirmed(context);

        LocalDateTime now = LocalDateTime.now();
        ExamAnswerSession session = findSession(planId, context.getExaminee().getId());
        if (session == null) {
            throw new BadRequestException("答题会话不存在");
        }
        if (isExpired(session, now)) {
            session = markTimeExpired(session, now);
            log.warn("traceNo={} event=candidate_answer_save_rejected candidateId={} planId={} sessionId={} paperQuestionId={} sessionStatus={} reason=time_expired",
                    TraceContext.getTraceNo(),
                    context.getExaminee().getId(),
                    planId,
                    session.getId(),
                    paperQuestionId,
                    session.getStatus());
            throw new ForbiddenException("答题时间已结束");
        }

        PaperQuestion paperQuestion = requireActivePaperQuestion(session.getPaperId(), paperQuestionId);
        ExamAnswerRecord record = examAnswerRecordMapper.selectOne(new LambdaQueryWrapper<ExamAnswerRecord>()
                .eq(ExamAnswerRecord::getSessionId, session.getId())
                .eq(ExamAnswerRecord::getPaperQuestionId, paperQuestionId)
                .last("limit 1"));

        String answerStatus = isAnswered(request == null ? null : request.getAnswerContent()) ? "ANSWERED" : "UNANSWERED";
        String answerContent = serializeAnswerContent(request == null ? null : request.getAnswerContent());

        if (record == null) {
            record = new ExamAnswerRecord();
            record.setSessionId(session.getId());
            record.setPaperQuestionId(paperQuestionId);
            record.setQuestionId(paperQuestion.getQuestionId());
            record.setCreatedAt(now);
        }
        record.setAnswerContent(answerContent);
        record.setAnswerStatus(answerStatus);
        record.setLastSavedAt(now);
        record.setUpdatedAt(now);

        if (record.getId() == null) {
            examAnswerRecordMapper.insert(record);
        } else {
            examAnswerRecordMapper.updateById(record);
        }

        session.setLastSavedAt(now);
        session.setUpdatedAt(now);
        examAnswerSessionMapper.updateById(session);

        int answeredCount = countAnswered(session.getId());
        log.info("traceNo={} event=candidate_answer_saved candidateId={} planId={} sessionId={} paperQuestionId={} answerStatus={} answeredCount={}",
                TraceContext.getTraceNo(),
                context.getExaminee().getId(),
                planId,
                session.getId(),
                paperQuestionId,
                answerStatus,
                answeredCount);

        return CandidateSaveAnswerResponse.builder()
                .paperQuestionId(paperQuestionId)
                .answerStatus(answerStatus)
                .lastSavedAt(now)
                .remainingSeconds(remainingSeconds(session.getDeadlineAt(), now))
                .sessionStatus(session.getStatus())
                .answeredCount(answeredCount)
                .build();
    }

    private void ensureProfileConfirmed(CandidateUserContext context) {
        if (!context.isProfileConfirmed()) {
            throw new ForbiddenException("请先确认身份信息");
        }
    }

    private ExamPlan requireCandidatePlan(Long planId, Long examineeId) {
        ExamPlan plan = examPlanMapper.selectById(planId);
        if (plan == null || plan.getDeleted() == null || plan.getDeleted() != 0 || !"PUBLISHED".equals(plan.getStatus())) {
            throw new ForbiddenException("当前考试不可进入答题");
        }

        ExamPlanExaminee relation = examPlanExamineeMapper.selectOne(new LambdaQueryWrapper<ExamPlanExaminee>()
                .eq(ExamPlanExaminee::getExamPlanId, planId)
                .eq(ExamPlanExaminee::getExamineeId, examineeId)
                .last("limit 1"));
        if (relation == null) {
            throw new ForbiddenException("当前考试不可进入答题");
        }
        return plan;
    }

    private Paper requireActivePaper(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null || paper.getDeleted() == null || paper.getDeleted() != 0) {
            throw new BadRequestException("试卷不存在");
        }
        return paper;
    }

    private PaperQuestion requireActivePaperQuestion(Long paperId, Long paperQuestionId) {
        PaperQuestion paperQuestion = paperQuestionMapper.selectOne(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getId, paperQuestionId)
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getDeleted, 0)
                .last("limit 1"));
        if (paperQuestion == null) {
            throw new BadRequestException("试题不存在");
        }
        return paperQuestion;
    }

    private ExamAnswerSession findSession(Long planId, Long examineeId) {
        return examAnswerSessionMapper.selectOne(new LambdaQueryWrapper<ExamAnswerSession>()
                .eq(ExamAnswerSession::getExamPlanId, planId)
                .eq(ExamAnswerSession::getExamineeId, examineeId)
                .last("limit 1"));
    }

    private ExamAnswerSession createSession(ExamPlan plan, Paper paper, Long examineeId, LocalDateTime now) {
        LocalDateTime candidateDeadline = now.plusMinutes(paper.getDurationMinutes());
        LocalDateTime deadlineAt = candidateDeadline.isBefore(plan.getEndTime()) ? candidateDeadline : plan.getEndTime();

        ExamAnswerSession session = new ExamAnswerSession();
        session.setExamPlanId(plan.getId());
        session.setExamineeId(examineeId);
        session.setPaperId(paper.getId());
        session.setStartedAt(now);
        session.setDeadlineAt(deadlineAt);
        session.setStatus(deadlineAt.isAfter(now) ? "IN_PROGRESS" : "TIME_EXPIRED");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        examAnswerSessionMapper.insert(session);
        return session;
    }

    private boolean isExpired(ExamAnswerSession session, LocalDateTime now) {
        return "TIME_EXPIRED".equals(session.getStatus()) || !session.getDeadlineAt().isAfter(now);
    }

    private ExamAnswerSession markTimeExpired(ExamAnswerSession session, LocalDateTime now) {
        if (!"TIME_EXPIRED".equals(session.getStatus())) {
            session.setStatus("TIME_EXPIRED");
            session.setUpdatedAt(now);
            examAnswerSessionMapper.updateById(session);
        }
        return session;
    }

    private long remainingSeconds(LocalDateTime deadlineAt, LocalDateTime now) {
        return Math.max(0, Duration.between(now, deadlineAt).getSeconds());
    }

    private Map<Long, ExamAnswerRecord> loadRecordMap(Long sessionId) {
        List<ExamAnswerRecord> records = examAnswerRecordMapper.selectList(new LambdaQueryWrapper<ExamAnswerRecord>()
                .eq(ExamAnswerRecord::getSessionId, sessionId));
        Map<Long, ExamAnswerRecord> recordMap = new HashMap<>();
        for (ExamAnswerRecord record : records) {
            recordMap.put(record.getPaperQuestionId(), record);
        }
        return recordMap;
    }

    private int countAnswered(Long sessionId) {
        return Math.toIntExact(examAnswerRecordMapper.selectCount(new LambdaQueryWrapper<ExamAnswerRecord>()
                .eq(ExamAnswerRecord::getSessionId, sessionId)
                .eq(ExamAnswerRecord::getAnswerStatus, "ANSWERED")));
    }

    private CandidateAnswerQuestionItemResponse toQuestionItem(CandidateAnswerQuestionView questionView, ExamAnswerRecord record) {
        return CandidateAnswerQuestionItemResponse.builder()
                .paperQuestionId(questionView.getPaperQuestionId())
                .questionId(questionView.getQuestionId())
                .questionNo(questionView.getQuestionNo())
                .stem(questionView.getStem())
                .questionTypeName(questionView.getQuestionTypeName())
                .answerMode(questionView.getAnswerMode())
                .answerConfig(sanitizeAnswerConfig(questionView.getAnswerConfig()))
                .score(questionView.getScore())
                .savedAnswer(parseJson(record == null ? null : record.getAnswerContent()))
                .answerStatus(record == null ? "UNANSWERED" : record.getAnswerStatus())
                .build();
    }

    private JsonNode sanitizeAnswerConfig(String rawAnswerConfig) {
        JsonNode answerConfig = parseJson(rawAnswerConfig);
        if (answerConfig instanceof ObjectNode objectNode) {
            objectNode.remove(List.of("correctOption", "correctOptions", "correctAnswer", "acceptedAnswers"));
        }
        return answerConfig;
    }

    private JsonNode parseJson(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawValue);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON 解析失败", exception);
        }
    }

    private String serializeAnswerContent(JsonNode answerContent) {
        if (answerContent == null || answerContent.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(answerContent);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON 序列化失败", exception);
        }
    }

    private boolean isAnswered(JsonNode answerContent) {
        if (answerContent == null || answerContent.isNull()) {
            return false;
        }
        if (answerContent.isTextual()) {
            return !answerContent.asText().trim().isEmpty();
        }
        if (answerContent.isArray()) {
            return answerContent.size() > 0;
        }
        if (answerContent.isObject()) {
            return answerContent.properties().stream().anyMatch(entry -> isAnswered(entry.getValue()));
        }
        return true;
    }
}
