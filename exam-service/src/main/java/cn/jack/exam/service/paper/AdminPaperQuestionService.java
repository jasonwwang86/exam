package cn.jack.exam.service.paper;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.paper.CreatePaperQuestionsRequest;
import cn.jack.exam.dto.paper.PaperQuestionResponse;
import cn.jack.exam.dto.paper.UpdatePaperQuestionRequest;
import cn.jack.exam.entity.PaperQuestion;
import cn.jack.exam.entity.Question;
import cn.jack.exam.entity.QuestionType;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.PaperQuestionMapper;
import cn.jack.exam.mapper.QuestionMapper;
import cn.jack.exam.service.question.AdminQuestionTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaperQuestionService {

    private final AdminPaperService adminPaperService;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final AdminQuestionTypeService adminQuestionTypeService;

    public List<PaperQuestionResponse> list(Long paperId) {
        adminPaperService.requireActive(paperId);
        return paperQuestionMapper.findByPaperId(paperId);
    }

    public List<PaperQuestionResponse> create(Long paperId, CreatePaperQuestionsRequest request) {
        adminPaperService.requireActive(paperId);

        List<PaperQuestionResponse> created = new ArrayList<>();
        int nextDisplayOrder = paperQuestionMapper.findMaxDisplayOrder(paperId) + 1;
        for (Long questionId : request.getQuestionIds()) {
            ensureQuestionNotDuplicated(paperId, questionId);
            Question question = requireActiveQuestion(questionId);
            QuestionType questionType = adminQuestionTypeService.requireActive(question.getQuestionTypeId());

            LocalDateTime now = LocalDateTime.now();
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paperId);
            paperQuestion.setQuestionId(question.getId());
            paperQuestion.setQuestionStemSnapshot(question.getStem());
            paperQuestion.setQuestionTypeNameSnapshot(questionType.getName());
            paperQuestion.setDifficultySnapshot(question.getDifficulty());
            paperQuestion.setAnswerConfigSnapshot(question.getAnswerConfig());
            paperQuestion.setItemScore(question.getScore());
            paperQuestion.setDisplayOrder(nextDisplayOrder++);
            paperQuestion.setDeleted(0);
            paperQuestion.setCreatedAt(now);
            paperQuestion.setUpdatedAt(now);
            paperQuestionMapper.insert(paperQuestion);

            log.info("traceNo={} event=paper_question_created paperId={} paperQuestionId={} questionId={} itemScore={}",
                    TraceContext.getTraceNo(),
                    paperId,
                    paperQuestion.getId(),
                    question.getId(),
                    paperQuestion.getItemScore());

            created.add(toResponse(paperQuestion));
        }

        adminPaperService.refreshTotalScore(paperId);
        return created;
    }

    public PaperQuestionResponse update(Long paperId, Long paperQuestionId, UpdatePaperQuestionRequest request) {
        adminPaperService.requireActive(paperId);
        PaperQuestion paperQuestion = requireActivePaperQuestion(paperId, paperQuestionId);
        paperQuestion.setItemScore(request.getItemScore());
        paperQuestion.setDisplayOrder(request.getDisplayOrder());
        paperQuestion.setUpdatedAt(LocalDateTime.now());
        paperQuestionMapper.updateById(paperQuestion);
        adminPaperService.refreshTotalScore(paperId);

        log.info("traceNo={} event=paper_question_updated paperId={} paperQuestionId={} questionId={} itemScore={} displayOrder={}",
                TraceContext.getTraceNo(),
                paperId,
                paperQuestion.getId(),
                paperQuestion.getQuestionId(),
                paperQuestion.getItemScore(),
                paperQuestion.getDisplayOrder());

        return toResponse(paperQuestion);
    }

    public void delete(Long paperId, Long paperQuestionId) {
        adminPaperService.requireActive(paperId);
        PaperQuestion paperQuestion = requireActivePaperQuestion(paperId, paperQuestionId);
        paperQuestion.setDeleted(1);
        paperQuestion.setUpdatedAt(LocalDateTime.now());
        paperQuestionMapper.updateById(paperQuestion);
        adminPaperService.refreshTotalScore(paperId);

        log.info("traceNo={} event=paper_question_deleted paperId={} paperQuestionId={} questionId={}",
                TraceContext.getTraceNo(),
                paperId,
                paperQuestion.getId(),
                paperQuestion.getQuestionId());
    }

    private Question requireActiveQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getDeleted() == null || question.getDeleted() != 0) {
            throw new BadRequestException("题目不存在");
        }
        return question;
    }

    private void ensureQuestionNotDuplicated(Long paperId, Long questionId) {
        PaperQuestion existing = paperQuestionMapper.selectOne(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getQuestionId, questionId)
                .eq(PaperQuestion::getDeleted, 0)
                .last("limit 1"));
        if (existing != null) {
            throw new BadRequestException("题目已存在于当前试卷中");
        }
    }

    private PaperQuestion requireActivePaperQuestion(Long paperId, Long paperQuestionId) {
        PaperQuestion paperQuestion = paperQuestionMapper.selectOne(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getId, paperQuestionId)
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getDeleted, 0)
                .last("limit 1"));
        if (paperQuestion == null) {
            throw new BadRequestException("试卷题目明细不存在");
        }
        return paperQuestion;
    }

    private PaperQuestionResponse toResponse(PaperQuestion paperQuestion) {
        return PaperQuestionResponse.builder()
                .id(paperQuestion.getId())
                .questionId(paperQuestion.getQuestionId())
                .questionStemSnapshot(paperQuestion.getQuestionStemSnapshot())
                .questionTypeNameSnapshot(paperQuestion.getQuestionTypeNameSnapshot())
                .difficultySnapshot(paperQuestion.getDifficultySnapshot())
                .itemScore(paperQuestion.getItemScore())
                .displayOrder(paperQuestion.getDisplayOrder())
                .updatedAt(paperQuestion.getUpdatedAt())
                .build();
    }
}
