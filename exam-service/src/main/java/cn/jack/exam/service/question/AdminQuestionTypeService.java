package cn.jack.exam.service.question;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.question.QuestionTypeResponse;
import cn.jack.exam.dto.question.SaveQuestionTypeRequest;
import cn.jack.exam.entity.QuestionType;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.QuestionMapper;
import cn.jack.exam.mapper.QuestionTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuestionTypeService {

    private final QuestionTypeMapper questionTypeMapper;
    private final QuestionMapper questionMapper;

    public List<QuestionTypeResponse> list() {
        return questionTypeMapper.findAllActive();
    }

    public QuestionTypeResponse create(SaveQuestionTypeRequest request) {
        validateUniqueName(null, request.getName());

        QuestionType questionType = new QuestionType();
        LocalDateTime now = LocalDateTime.now();
        questionType.setName(request.getName().trim());
        questionType.setAnswerMode(request.getAnswerMode());
        questionType.setSort(request.getSort());
        questionType.setRemark(request.getRemark());
        questionType.setDeleted(0);
        questionType.setCreatedAt(now);
        questionType.setUpdatedAt(now);
        questionTypeMapper.insert(questionType);

        log.info("traceNo={} event=question_type_created questionTypeId={} questionTypeName={}",
                TraceContext.getTraceNo(),
                questionType.getId(),
                questionType.getName());

        return toResponse(questionType);
    }

    public QuestionTypeResponse update(Long id, SaveQuestionTypeRequest request) {
        QuestionType questionType = requireActive(id);
        validateUniqueName(id, request.getName());

        questionType.setName(request.getName().trim());
        questionType.setAnswerMode(request.getAnswerMode());
        questionType.setSort(request.getSort());
        questionType.setRemark(request.getRemark());
        questionType.setUpdatedAt(LocalDateTime.now());
        questionTypeMapper.updateById(questionType);

        log.info("traceNo={} event=question_type_updated questionTypeId={} questionTypeName={}",
                TraceContext.getTraceNo(),
                questionType.getId(),
                questionType.getName());

        return toResponse(questionType);
    }

    public void delete(Long id) {
        QuestionType questionType = requireActive(id);
        if (questionMapper.countActiveByQuestionTypeId(id) > 0) {
            throw new BadRequestException("题型已被题目引用，无法删除");
        }

        questionType.setDeleted(1);
        questionType.setUpdatedAt(LocalDateTime.now());
        questionTypeMapper.updateById(questionType);

        log.info("traceNo={} event=question_type_deleted questionTypeId={} questionTypeName={}",
                TraceContext.getTraceNo(),
                questionType.getId(),
                questionType.getName());
    }

    public QuestionType requireActive(Long id) {
        QuestionType questionType = questionTypeMapper.selectById(id);
        if (questionType == null || questionType.getDeleted() == null || questionType.getDeleted() != 0) {
            throw new BadRequestException("题型不存在");
        }
        return questionType;
    }

    private void validateUniqueName(Long currentId, String name) {
        QuestionType existing = questionTypeMapper.selectOne(new LambdaQueryWrapper<QuestionType>()
                .eq(QuestionType::getName, name.trim())
                .eq(QuestionType::getDeleted, 0)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BadRequestException("题型名称已存在");
        }
    }

    private QuestionTypeResponse toResponse(QuestionType questionType) {
        return QuestionTypeResponse.builder()
                .id(questionType.getId())
                .name(questionType.getName())
                .answerMode(questionType.getAnswerMode())
                .sort(questionType.getSort())
                .remark(questionType.getRemark())
                .build();
    }
}
