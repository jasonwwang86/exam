package cn.jack.exam.service.candidate;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.entity.ExamAnswerRecord;
import cn.jack.exam.entity.ExamAnswerSession;
import cn.jack.exam.entity.ExamResult;
import cn.jack.exam.entity.ExamResultItem;
import cn.jack.exam.entity.PaperQuestion;
import cn.jack.exam.mapper.ExamAnswerRecordMapper;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamResultItemMapper;
import cn.jack.exam.mapper.ExamResultMapper;
import cn.jack.exam.mapper.PaperQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateScoreGenerationService {

    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AUTO_SUBMITTED = "AUTO_SUBMITTED";
    private static final String SCORE_STATUS_PUBLISHED = "PUBLISHED";
    private static final String ANSWERED = "ANSWERED";
    private static final String UNANSWERED = "UNANSWERED";
    private static final String CORRECT = "CORRECT";
    private static final String WRONG = "WRONG";

    private final ExamAnswerSessionMapper examAnswerSessionMapper;
    private final ExamAnswerRecordMapper examAnswerRecordMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamResultMapper examResultMapper;
    private final ExamResultItemMapper examResultItemMapper;
    private final ObjectMapper objectMapper;

    public void generateForSession(ExamAnswerSession session) {
        if (session == null || !isFinalSubmitted(session.getStatus())) {
            return;
        }

        ExistingResultSnapshot existingSnapshot = snapshotExistingResult(session);
        try {
            doGenerate(session);
        } catch (RuntimeException exception) {
            restoreExistingResult(session, existingSnapshot);
            log.warn("traceNo={} event=candidate_score_generation_failed candidateId={} planId={} sessionId={} message={}",
                    TraceContext.getTraceNo(),
                    session.getExamineeId(),
                    session.getExamPlanId(),
                    session.getId(),
                    exception.getMessage());
            throw exception;
        }
    }

    @Scheduled(fixedDelayString = "${exam.candidate.score-generation.fixed-delay-ms:10000}")
    public void retryPendingScoreGeneration() {
        List<ExamAnswerSession> finalSessions = examAnswerSessionMapper.selectList(new LambdaQueryWrapper<ExamAnswerSession>()
                .in(ExamAnswerSession::getStatus, STATUS_SUBMITTED, STATUS_AUTO_SUBMITTED));
        for (ExamAnswerSession session : finalSessions) {
            long resultCount = examResultMapper.selectCount(new LambdaQueryWrapper<ExamResult>()
                    .eq(ExamResult::getExamPlanId, session.getExamPlanId())
                    .eq(ExamResult::getExamineeId, session.getExamineeId()));
            if (resultCount > 0) {
                continue;
            }
            try {
                generateForSession(session);
                log.info("traceNo={} event=candidate_score_generation_retried candidateId={} planId={} sessionId={}",
                        TraceContext.getTraceNo(),
                        session.getExamineeId(),
                        session.getExamPlanId(),
                        session.getId());
            } catch (RuntimeException exception) {
                log.warn("traceNo={} event=candidate_score_generation_retry_failed candidateId={} planId={} sessionId={} message={}",
                        TraceContext.getTraceNo(),
                        session.getExamineeId(),
                        session.getExamPlanId(),
                        session.getId(),
                        exception.getMessage());
            }
        }
    }

    private void doGenerate(ExamAnswerSession session) {
        List<PaperQuestion> questions = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, session.getPaperId())
                .eq(PaperQuestion::getDeleted, 0)
                .orderByAsc(PaperQuestion::getDisplayOrder));

        Map<Long, ExamAnswerRecord> recordByQuestionId = new HashMap<>();
        examAnswerRecordMapper.selectList(new LambdaQueryWrapper<ExamAnswerRecord>()
                        .eq(ExamAnswerRecord::getSessionId, session.getId()))
                .forEach(record -> recordByQuestionId.put(record.getPaperQuestionId(), record));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal objectiveScore = BigDecimal.ZERO;
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        int answeredCount = 0;
        List<ExamResultItem> items = new ArrayList<>();

        for (PaperQuestion question : questions) {
            ExamAnswerRecord record = recordByQuestionId.get(question.getId());
            JudgedItem judgedItem = judge(question, record, now);
            items.add(judgedItem.item());
            totalScore = totalScore.add(judgedItem.item().getAwardedScore());
            if (ANSWERED.equals(judgedItem.item().getAnswerStatus())) {
                answeredCount++;
            }
            if ("TEXT".equals(resolveAnswerMode(question))) {
                subjectiveScore = subjectiveScore.add(judgedItem.item().getAwardedScore());
            } else {
                objectiveScore = objectiveScore.add(judgedItem.item().getAwardedScore());
            }
        }

        ExamResult result = examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, session.getExamPlanId())
                .eq(ExamResult::getExamineeId, session.getExamineeId())
                .last("limit 1"));

        if (result == null) {
            result = new ExamResult();
            result.setExamPlanId(session.getExamPlanId());
            result.setExamineeId(session.getExamineeId());
            result.setCreatedAt(now);
        }
        result.setSessionId(session.getId());
        result.setPaperId(session.getPaperId());
        result.setScoreStatus(SCORE_STATUS_PUBLISHED);
        result.setTotalScore(totalScore);
        result.setObjectiveScore(objectiveScore);
        result.setSubjectiveScore(subjectiveScore);
        result.setAnsweredCount(answeredCount);
        result.setUnansweredCount(Math.max(0, questions.size() - answeredCount));
        result.setSubmittedAt(session.getSubmittedAt() == null ? now : session.getSubmittedAt());
        result.setGeneratedAt(now);
        result.setPublishedAt(now);
        result.setUpdatedAt(now);

        if (result.getId() == null) {
            examResultMapper.insert(result);
        } else {
            examResultMapper.updateById(result);
            examResultItemMapper.delete(new LambdaQueryWrapper<ExamResultItem>()
                    .eq(ExamResultItem::getResultId, result.getId()));
        }

        for (ExamResultItem item : items.stream().sorted(Comparator.comparing(ExamResultItem::getQuestionNo)).toList()) {
            item.setResultId(result.getId());
            examResultItemMapper.insert(item);
        }

        log.info("traceNo={} event=candidate_score_generated candidateId={} planId={} sessionId={} resultId={} totalScore={} itemCount={}",
                TraceContext.getTraceNo(),
                session.getExamineeId(),
                session.getExamPlanId(),
                session.getId(),
                result.getId(),
                totalScore,
                items.size());
    }

    private JudgedItem judge(PaperQuestion question, ExamAnswerRecord record, LocalDateTime now) {
        String answerMode = resolveAnswerMode(question);
        BigDecimal fullScore = question.getItemScore() == null ? BigDecimal.ZERO : question.getItemScore();
        JsonNode answerConfig = parseJson(question.getAnswerConfigSnapshot());
        JsonNode answerContent = parseJson(record == null ? null : record.getAnswerContent());
        boolean answered = record != null && ANSWERED.equals(record.getAnswerStatus()) && answerContent != null && !answerContent.isNull();

        BigDecimal awardedScore = BigDecimal.ZERO;
        String judgeStatus = answered ? WRONG : UNANSWERED;
        String answerSummary = buildAnswerSummary(answerMode, answerContent);

        if (answered && isCorrect(answerMode, answerConfig, answerContent)) {
            awardedScore = fullScore;
            judgeStatus = CORRECT;
        }

        ExamResultItem item = new ExamResultItem();
        item.setPaperQuestionId(question.getId());
        item.setQuestionId(question.getQuestionId());
        item.setQuestionNo(question.getDisplayOrder());
        item.setQuestionStemSnapshot(question.getQuestionStemSnapshot());
        item.setQuestionTypeNameSnapshot(question.getQuestionTypeNameSnapshot());
        item.setItemScore(fullScore);
        item.setAwardedScore(awardedScore);
        item.setAnswerStatus(answered ? ANSWERED : UNANSWERED);
        item.setAnswerSummary(answerSummary);
        item.setJudgeStatus(judgeStatus);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return new JudgedItem(item);
    }

    private boolean isCorrect(String answerMode, JsonNode answerConfig, JsonNode answerContent) {
        if (answerContent == null || answerContent.isNull()) {
            return false;
        }
        return switch (answerMode) {
            case "SINGLE_CHOICE" -> Objects.equals(textValue(answerContent.get("selectedOption")), textValue(answerConfig.get("correctOption")));
            case "MULTIPLE_CHOICE" -> normalizeTextList(answerContent.get("selectedOptions"))
                    .equals(normalizeTextList(answerConfig.get("correctOptions")));
            case "TRUE_FALSE" -> Objects.equals(booleanValue(answerContent.get("selectedAnswer")), booleanValue(answerConfig.get("correctAnswer")));
            case "TEXT" -> {
                String normalizedAnswer = normalizeText(textValue(answerContent.get("textAnswer")));
                yield !normalizedAnswer.isBlank() && normalizeTextList(answerConfig.get("acceptedAnswers")).contains(normalizedAnswer);
            }
            default -> false;
        };
    }

    private String buildAnswerSummary(String answerMode, JsonNode answerContent) {
        if (answerContent == null || answerContent.isNull()) {
            return null;
        }
        return switch (answerMode) {
            case "SINGLE_CHOICE" -> {
                String selectedOption = textValue(answerContent.get("selectedOption"));
                yield selectedOption == null || selectedOption.isBlank() ? null : "选择 " + selectedOption;
            }
            case "MULTIPLE_CHOICE" -> {
                List<String> options = normalizeTextList(answerContent.get("selectedOptions"));
                yield options.isEmpty() ? null : "选择 " + String.join(",", options);
            }
            case "TRUE_FALSE" -> {
                Boolean selectedAnswer = booleanValue(answerContent.get("selectedAnswer"));
                yield selectedAnswer == null ? null : (selectedAnswer ? "判断为 true" : "判断为 false");
            }
            case "TEXT" -> {
                String textAnswer = textValue(answerContent.get("textAnswer"));
                yield textAnswer == null || textAnswer.isBlank() ? null : textAnswer.trim();
            }
            default -> null;
        };
    }

    private String resolveAnswerMode(PaperQuestion question) {
        String typeName = question.getQuestionTypeNameSnapshot();
        if ("单选题".equals(typeName)) {
            return "SINGLE_CHOICE";
        }
        if ("多选题".equals(typeName)) {
            return "MULTIPLE_CHOICE";
        }
        if ("判断题".equals(typeName)) {
            return "TRUE_FALSE";
        }
        if ("简答题".equals(typeName)) {
            return "TEXT";
        }
        JsonNode answerConfig = parseJson(question.getAnswerConfigSnapshot());
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

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }

    private Boolean booleanValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isTextual()) {
            String rawValue = node.asText().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(rawValue)) {
                return true;
            }
            if ("false".equals(rawValue)) {
                return false;
            }
        }
        return null;
    }

    private List<String> normalizeTextList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isNull() || !node.isArray()) {
            return values;
        }
        node.forEach(item -> {
            String normalized = normalizeText(textValue(item));
            if (!normalized.isBlank() && !values.contains(normalized)) {
                values.add(normalized);
            }
        });
        values.sort(String::compareTo);
        return values;
    }

    private String normalizeText(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        return rawValue.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private ExistingResultSnapshot snapshotExistingResult(ExamAnswerSession session) {
        ExamResult existing = examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, session.getExamPlanId())
                .eq(ExamResult::getExamineeId, session.getExamineeId())
                .last("limit 1"));
        if (existing == null) {
            return new ExistingResultSnapshot(null, List.of());
        }
        List<ExamResultItem> items = examResultItemMapper.selectList(new LambdaQueryWrapper<ExamResultItem>()
                .eq(ExamResultItem::getResultId, existing.getId())
                .orderByAsc(ExamResultItem::getQuestionNo));
        return new ExistingResultSnapshot(copyResult(existing), items.stream().map(this::copyItem).toList());
    }

    private void restoreExistingResult(ExamAnswerSession session, ExistingResultSnapshot existingSnapshot) {
        ExamResult current = examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, session.getExamPlanId())
                .eq(ExamResult::getExamineeId, session.getExamineeId())
                .last("limit 1"));
        if (current != null) {
            examResultItemMapper.delete(new LambdaQueryWrapper<ExamResultItem>()
                    .eq(ExamResultItem::getResultId, current.getId()));
        }

        if (existingSnapshot.result() == null) {
            if (current != null) {
                examResultMapper.deleteById(current.getId());
            }
            return;
        }

        ExamResult result = existingSnapshot.result();
        if (current == null) {
            examResultMapper.insert(result);
        } else {
            result.setId(current.getId());
            examResultMapper.updateById(result);
        }

        Long resultId = result.getId();
        for (ExamResultItem item : existingSnapshot.items()) {
            item.setId(null);
            item.setResultId(resultId);
            examResultItemMapper.insert(item);
        }
    }

    private ExamResult copyResult(ExamResult source) {
        ExamResult target = new ExamResult();
        target.setId(source.getId());
        target.setExamPlanId(source.getExamPlanId());
        target.setExamineeId(source.getExamineeId());
        target.setSessionId(source.getSessionId());
        target.setPaperId(source.getPaperId());
        target.setScoreStatus(source.getScoreStatus());
        target.setTotalScore(source.getTotalScore());
        target.setObjectiveScore(source.getObjectiveScore());
        target.setSubjectiveScore(source.getSubjectiveScore());
        target.setAnsweredCount(source.getAnsweredCount());
        target.setUnansweredCount(source.getUnansweredCount());
        target.setSubmittedAt(source.getSubmittedAt());
        target.setGeneratedAt(source.getGeneratedAt());
        target.setPublishedAt(source.getPublishedAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private ExamResultItem copyItem(ExamResultItem source) {
        ExamResultItem target = new ExamResultItem();
        target.setId(source.getId());
        target.setResultId(source.getResultId());
        target.setPaperQuestionId(source.getPaperQuestionId());
        target.setQuestionId(source.getQuestionId());
        target.setQuestionNo(source.getQuestionNo());
        target.setQuestionStemSnapshot(source.getQuestionStemSnapshot());
        target.setQuestionTypeNameSnapshot(source.getQuestionTypeNameSnapshot());
        target.setItemScore(source.getItemScore());
        target.setAwardedScore(source.getAwardedScore());
        target.setAnswerStatus(source.getAnswerStatus());
        target.setAnswerSummary(source.getAnswerSummary());
        target.setJudgeStatus(source.getJudgeStatus());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private boolean isFinalSubmitted(String status) {
        return STATUS_SUBMITTED.equals(status) || STATUS_AUTO_SUBMITTED.equals(status);
    }

    private record JudgedItem(ExamResultItem item) {
    }

    private record ExistingResultSnapshot(ExamResult result, List<ExamResultItem> items) {
    }
}
