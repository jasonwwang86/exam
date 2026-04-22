package cn.jack.exam.service.question;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.question.QuestionDetailResponse;
import cn.jack.exam.dto.question.QuestionListItemResponse;
import cn.jack.exam.dto.question.QuestionPageResponse;
import cn.jack.exam.dto.question.SaveQuestionRequest;
import cn.jack.exam.entity.Question;
import cn.jack.exam.entity.QuestionType;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.QuestionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private final QuestionMapper questionMapper;
    private final AdminQuestionTypeService adminQuestionTypeService;
    private final ObjectMapper objectMapper;

    public QuestionPageResponse list(String keyword, Long questionTypeId, String difficulty, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safePage - 1) * safePageSize;
        List<QuestionListItemResponse> records = questionMapper.findPage(keyword, questionTypeId, difficulty, offset, safePageSize);
        long total = questionMapper.countPage(keyword, questionTypeId, difficulty);
        return QuestionPageResponse.builder()
                .page(safePage)
                .pageSize(safePageSize)
                .total(total)
                .records(records)
                .build();
    }

    public QuestionDetailResponse get(Long id) {
        return toDetailResponse(requireActive(id));
    }

    public QuestionDetailResponse create(SaveQuestionRequest request) {
        QuestionType questionType = adminQuestionTypeService.requireActive(request.getQuestionTypeId());
        validateAnswerConfig(questionType.getAnswerMode(), request.getAnswerConfig());

        Question question = new Question();
        LocalDateTime now = LocalDateTime.now();
        question.setStem(request.getStem().trim());
        question.setQuestionTypeId(questionType.getId());
        question.setDifficulty(request.getDifficulty());
        question.setScore(request.getScore());
        question.setAnswerConfig(writeAnswerConfig(request.getAnswerConfig()));
        question.setDeleted(0);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        questionMapper.insert(question);

        log.info("traceNo={} event=question_created questionId={} questionTypeId={} difficulty={} score={}",
                TraceContext.getTraceNo(),
                question.getId(),
                question.getQuestionTypeId(),
                question.getDifficulty(),
                question.getScore());

        return toDetailResponse(question, questionType);
    }

    public QuestionDetailResponse update(Long id, SaveQuestionRequest request) {
        Question question = requireActive(id);
        QuestionType questionType = adminQuestionTypeService.requireActive(request.getQuestionTypeId());
        validateAnswerConfig(questionType.getAnswerMode(), request.getAnswerConfig());

        question.setStem(request.getStem().trim());
        question.setQuestionTypeId(questionType.getId());
        question.setDifficulty(request.getDifficulty());
        question.setScore(request.getScore());
        question.setAnswerConfig(writeAnswerConfig(request.getAnswerConfig()));
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(question);

        log.info("traceNo={} event=question_updated questionId={} questionTypeId={} difficulty={} score={}",
                TraceContext.getTraceNo(),
                question.getId(),
                question.getQuestionTypeId(),
                question.getDifficulty(),
                question.getScore());

        return toDetailResponse(question, questionType);
    }

    public void delete(Long id) {
        Question question = requireActive(id);
        question.setDeleted(1);
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(question);

        log.info("traceNo={} event=question_deleted questionId={} questionTypeId={}",
                TraceContext.getTraceNo(),
                question.getId(),
                question.getQuestionTypeId());
    }

    private Question requireActive(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null || question.getDeleted() == null || question.getDeleted() != 0) {
            throw new BadRequestException("题目不存在");
        }
        return question;
    }

    private QuestionDetailResponse toDetailResponse(Question question) {
        QuestionType questionType = adminQuestionTypeService.requireActive(question.getQuestionTypeId());
        return toDetailResponse(question, questionType);
    }

    private QuestionDetailResponse toDetailResponse(Question question, QuestionType questionType) {
        return QuestionDetailResponse.builder()
                .id(question.getId())
                .stem(question.getStem())
                .questionTypeId(question.getQuestionTypeId())
                .questionTypeName(questionType.getName())
                .answerMode(questionType.getAnswerMode())
                .difficulty(question.getDifficulty())
                .score(question.getScore())
                .answerConfig(readAnswerConfig(question.getAnswerConfig()))
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private String writeAnswerConfig(JsonNode answerConfig) {
        try {
            return objectMapper.writeValueAsString(answerConfig);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("答案配置与题型不匹配");
        }
    }

    private JsonNode readAnswerConfig(String answerConfig) {
        try {
            return objectMapper.readTree(answerConfig);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("题目答案配置解析失败");
        }
    }

    private void validateAnswerConfig(String answerMode, JsonNode answerConfig) {
        if (answerConfig == null || !answerConfig.isObject()) {
            throw new BadRequestException("答案配置与题型不匹配");
        }

        switch (answerMode) {
            case "SINGLE_CHOICE" -> validateSingleChoice(answerConfig);
            case "MULTIPLE_CHOICE" -> validateMultipleChoice(answerConfig);
            case "TRUE_FALSE" -> validateTrueFalse(answerConfig);
            case "TEXT" -> validateText(answerConfig);
            default -> throw new BadRequestException("答案配置与题型不匹配");
        }
    }

    private void validateSingleChoice(JsonNode answerConfig) {
        JsonNode options = answerConfig.get("options");
        JsonNode correctOption = answerConfig.get("correctOption");
        if (!isValidOptions(options) || correctOption == null || !correctOption.isTextual()) {
            throw new BadRequestException("答案配置与题型不匹配");
        }
        if (!containsOptionKey(options, correctOption.asText())) {
            throw new BadRequestException("答案配置与题型不匹配");
        }
    }

    private void validateMultipleChoice(JsonNode answerConfig) {
        JsonNode options = answerConfig.get("options");
        JsonNode correctOptions = answerConfig.get("correctOptions");
        if (!isValidOptions(options) || correctOptions == null || !correctOptions.isArray() || correctOptions.isEmpty()) {
            throw new BadRequestException("答案配置与题型不匹配");
        }
        for (JsonNode node : correctOptions) {
            if (!node.isTextual() || !containsOptionKey(options, node.asText())) {
                throw new BadRequestException("答案配置与题型不匹配");
            }
        }
    }

    private void validateTrueFalse(JsonNode answerConfig) {
        JsonNode correctAnswer = answerConfig.get("correctAnswer");
        if (correctAnswer == null || !correctAnswer.isBoolean()) {
            throw new BadRequestException("答案配置与题型不匹配");
        }
    }

    private void validateText(JsonNode answerConfig) {
        JsonNode acceptedAnswers = answerConfig.get("acceptedAnswers");
        if (acceptedAnswers == null || !acceptedAnswers.isArray() || acceptedAnswers.isEmpty()) {
            throw new BadRequestException("答案配置与题型不匹配");
        }

        Iterator<JsonNode> iterator = acceptedAnswers.elements();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (!node.isTextual() || node.asText().trim().isEmpty()) {
                throw new BadRequestException("答案配置与题型不匹配");
            }
        }
    }

    private boolean isValidOptions(JsonNode options) {
        if (options == null || !options.isArray() || options.size() < 2) {
            return false;
        }
        for (JsonNode option : options) {
            JsonNode key = option.get("key");
            JsonNode content = option.get("content");
            if (key == null || !key.isTextual() || key.asText().trim().isEmpty()) {
                return false;
            }
            if (content == null || !content.isTextual() || content.asText().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean containsOptionKey(JsonNode options, String targetKey) {
        for (JsonNode option : options) {
            JsonNode key = option.get("key");
            if (key != null && key.isTextual() && key.asText().equals(targetKey)) {
                return true;
            }
        }
        return false;
    }
}
