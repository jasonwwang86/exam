package cn.jack.exam.service.paper;

import cn.jack.exam.dto.paper.CreatePaperQuestionsRequest;
import cn.jack.exam.dto.paper.UpdatePaperQuestionRequest;
import cn.jack.exam.entity.PaperQuestion;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.PaperQuestionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminPaperQuestionServiceTest {

    @Autowired
    private AdminPaperQuestionService adminPaperQuestionService;

    @Autowired
    private AdminPaperService adminPaperService;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Test
    void shouldCreatePaperQuestionsAndRefreshPaperTotalScore() {
        CreatePaperQuestionsRequest request = new CreatePaperQuestionsRequest();
        request.setQuestionIds(List.of(1L, 3L));

        var responses = adminPaperQuestionService.create(2L, request);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(responses.get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(adminPaperService.get(2L).getTotalScore()).isEqualByComparingTo("7.00");
    }

    @Test
    void shouldRejectDuplicateQuestionInPaper() {
        CreatePaperQuestionsRequest request = new CreatePaperQuestionsRequest();
        request.setQuestionIds(List.of(1L));

        assertThatThrownBy(() -> adminPaperQuestionService.create(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("题目已存在于当前试卷中");
    }

    @Test
    void shouldUpdatePaperQuestionAndRefreshPaperTotalScore() {
        UpdatePaperQuestionRequest request = new UpdatePaperQuestionRequest();
        request.setItemScore(new BigDecimal("10.00"));
        request.setDisplayOrder(1);

        var response = adminPaperQuestionService.update(1L, 2L, request);

        assertThat(response.getItemScore()).isEqualByComparingTo("10.00");
        assertThat(response.getDisplayOrder()).isEqualTo(1);
        assertThat(adminPaperService.get(1L).getTotalScore()).isEqualByComparingTo("15.00");
    }

    @Test
    void shouldSoftDeletePaperQuestionAndRefreshPaperTotalScore() {
        adminPaperQuestionService.delete(1L, 2L);

        PaperQuestion paperQuestion = paperQuestionMapper.selectById(2L);
        assertThat(paperQuestion).isNotNull();
        assertThat(paperQuestion.getDeleted()).isEqualTo(1);
        assertThat(adminPaperService.get(1L).getTotalScore()).isEqualByComparingTo("5.00");
    }
}
