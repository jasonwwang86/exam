package cn.jack.exam.service.candidate;

import cn.jack.exam.config.CandidateUserContext;
import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.candidate.CandidateAnswerQuestionItemResponse;
import cn.jack.exam.dto.candidate.CandidateAnswerQuestionView;
import cn.jack.exam.dto.candidate.CandidateAnswerSessionResponse;
import cn.jack.exam.dto.candidate.CandidateExamSubmissionResponse;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateAnsweringService {

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_TIME_EXPIRED = "TIME_EXPIRED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AUTO_SUBMITTED = "AUTO_SUBMITTED";
    private static final String METHOD_MANUAL = "MANUAL";
    private static final String METHOD_AUTO = "AUTO";

    private final ExamPlanMapper examPlanMapper;
    private final ExamPlanExamineeMapper examPlanExamineeMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamAnswerSessionMapper examAnswerSessionMapper;
    private final ExamAnswerRecordMapper examAnswerRecordMapper;
    private final CandidateScoreGenerationService candidateScoreGenerationService;
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
        } else {
            session = normalizeSessionForRead(session, now);
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
                .submittedAt(session.getSubmittedAt())
                .submissionMethod(resolveSubmissionMethod(session.getStatus()))
                .questions(questionViews.stream()
                        .map(question -> toQuestionItem(question, recordByQuestionId.get(question.getPaperQuestionId())))
                        .toList())
                .build();
    }

    @Transactional
    public CandidateExamSubmissionResponse submitExam(Long planId, CandidateUserContext context) {
        ensureProfileConfirmed(context);

        LocalDateTime now = LocalDateTime.now();
        ExamPlan plan = requireCandidatePlan(planId, context.getExaminee().getId());
        Paper paper = requireActivePaper(plan.getPaperId());
        ExamAnswerSession session = findSession(planId, context.getExaminee().getId());
        if (session == null) {
            throw new BadRequestException("答题会话不存在");
        }

        session = normalizeSessionForRead(session, now);
        if (isFinalSubmitted(session)) {
            log.info("traceNo={} event=candidate_exam_submission_duplicate candidateId={} planId={} sessionId={} sessionStatus={}",
                    TraceContext.getTraceNo(),
                    context.getExaminee().getId(),
                    planId,
                    session.getId(),
                    session.getStatus());
            return buildSubmissionResponse(plan, paper, session);
        }

        session = finalizeSubmission(session, STATUS_SUBMITTED, now);
        log.info("traceNo={} event=candidate_exam_submitted candidateId={} planId={} sessionId={} sessionStatus={} submissionMethod={}",
                TraceContext.getTraceNo(),
                context.getExaminee().getId(),
                planId,
                session.getId(),
                session.getStatus(),
                resolveSubmissionMethod(session.getStatus()));
        return buildSubmissionResponse(plan, paper, session);
    }

    @Scheduled(fixedDelayString = "${exam.candidate.auto-submit.fixed-delay-ms:10000}")
    @Transactional
    public void autoSubmitExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamAnswerSession> expiredSessions = examAnswerSessionMapper.selectList(new LambdaQueryWrapper<ExamAnswerSession>()
                .eq(ExamAnswerSession::getStatus, STATUS_IN_PROGRESS)
                .le(ExamAnswerSession::getDeadlineAt, now));
        for (ExamAnswerSession session : expiredSessions) {
            finalizeSubmission(session, STATUS_AUTO_SUBMITTED, now);
            log.info("traceNo={} event=candidate_exam_submitted candidateId={} planId={} sessionId={} sessionStatus={} submissionMethod={}",
                    TraceContext.getTraceNo(),
                    session.getExamineeId(),
                    session.getExamPlanId(),
                    session.getId(),
                    session.getStatus(),
                    resolveSubmissionMethod(session.getStatus()));
        }
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
        if (isFinalSubmitted(session)) {
            log.warn("traceNo={} event=candidate_answer_save_rejected candidateId={} planId={} sessionId={} paperQuestionId={} sessionStatus={} reason=already_submitted",
                    TraceContext.getTraceNo(),
                    context.getExaminee().getId(),
                    planId,
                    session.getId(),
                    paperQuestionId,
                    session.getStatus());
            throw new ForbiddenException("试卷已提交，不能重复作答");
        }
        if (STATUS_TIME_EXPIRED.equals(session.getStatus())) {
            log.warn("traceNo={} event=candidate_answer_save_rejected candidateId={} planId={} sessionId={} paperQuestionId={} sessionStatus={} reason=time_expired",
                    TraceContext.getTraceNo(),
                    context.getExaminee().getId(),
                    planId,
                    session.getId(),
                    paperQuestionId,
                    session.getStatus());
            throw new ForbiddenException("答题时间已结束");
        }
        if (isExpiredByDeadline(session, now)) {
            session = finalizeSubmission(session, STATUS_AUTO_SUBMITTED, now);
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

    public ExamAnswerSession normalizeSessionForRead(ExamAnswerSession session, LocalDateTime now) {
        if (session == null) {
            return null;
        }
        if (isFinalSubmitted(session)) {
            return ensureSubmittedAt(session, now);
        }
        if (STATUS_TIME_EXPIRED.equals(session.getStatus()) || isExpiredByDeadline(session, now)) {
            return finalizeSubmission(session, STATUS_AUTO_SUBMITTED, now);
        }
        return session;
    }

    public boolean isFinalSubmitted(ExamAnswerSession session) {
        return session != null && isFinalStatus(session.getStatus());
    }

    public String resolveSubmissionMethod(String sessionStatus) {
        if (STATUS_SUBMITTED.equals(sessionStatus)) {
            return METHOD_MANUAL;
        }
        if (STATUS_AUTO_SUBMITTED.equals(sessionStatus)) {
            return METHOD_AUTO;
        }
        return null;
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
        session.setStatus(deadlineAt.isAfter(now) ? STATUS_IN_PROGRESS : STATUS_AUTO_SUBMITTED);
        if (!deadlineAt.isAfter(now)) {
            session.setSubmittedAt(now);
        }
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        examAnswerSessionMapper.insert(session);
        return session;
    }

    private boolean isExpired(ExamAnswerSession session, LocalDateTime now) {
        return STATUS_TIME_EXPIRED.equals(session.getStatus()) || isExpiredByDeadline(session, now);
    }

    private boolean isExpiredByDeadline(ExamAnswerSession session, LocalDateTime now) {
        return !session.getDeadlineAt().isAfter(now);
    }

    private boolean isFinalStatus(String status) {
        return STATUS_SUBMITTED.equals(status) || STATUS_AUTO_SUBMITTED.equals(status);
    }

    private ExamAnswerSession ensureSubmittedAt(ExamAnswerSession session, LocalDateTime now) {
        if (session.getSubmittedAt() == null) {
            LocalDateTime submittedAt = session.getLastSavedAt() != null ? session.getLastSavedAt() : now;
            session.setSubmittedAt(submittedAt);
            session.setUpdatedAt(now);
            examAnswerSessionMapper.updateById(session);
        }
        return session;
    }

    private ExamAnswerSession finalizeSubmission(ExamAnswerSession session, String finalStatus, LocalDateTime now) {
        if (isFinalSubmitted(session)) {
            return ensureSubmittedAt(session, now);
        }
        session.setStatus(finalStatus);
        session.setSubmittedAt(now);
        session.setUpdatedAt(now);
        examAnswerSessionMapper.updateById(session);
        triggerScoreGeneration(session);
        return session;
    }

    private void triggerScoreGeneration(ExamAnswerSession session) {
        try {
            candidateScoreGenerationService.generateForSession(session);
        } catch (RuntimeException exception) {
            log.warn("traceNo={} event=candidate_score_generation_deferred candidateId={} planId={} sessionId={} message={}",
                    TraceContext.getTraceNo(),
                    session.getExamineeId(),
                    session.getExamPlanId(),
                    session.getId(),
                    exception.getMessage());
        }
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

    private int countTotalQuestions(Long paperId) {
        return Math.toIntExact(paperQuestionMapper.selectCount(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getDeleted, 0)));
    }

    private CandidateExamSubmissionResponse buildSubmissionResponse(ExamPlan plan, Paper paper, ExamAnswerSession session) {
        return CandidateExamSubmissionResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .paperName(paper.getName())
                .sessionStatus(session.getStatus())
                .submissionMethod(resolveSubmissionMethod(session.getStatus()))
                .submittedAt(session.getSubmittedAt())
                .answeredCount(countAnswered(session.getId()))
                .totalQuestionCount(countTotalQuestions(session.getPaperId()))
                .build();
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
