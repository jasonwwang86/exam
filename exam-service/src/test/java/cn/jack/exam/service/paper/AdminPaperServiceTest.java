package cn.jack.exam.service.paper;

import cn.jack.exam.dto.paper.SavePaperRequest;
import cn.jack.exam.entity.Paper;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.PaperMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminPaperServiceTest {

    @Autowired
    private AdminPaperService adminPaperService;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreatePaperAndNormalizeBlankOptionalFields() {
        var response = adminPaperService.create(saveRequest("  新试卷  ", "   ", 60, " "));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("新试卷");
        assertThat(response.getDescription()).isNull();
        assertThat(response.getRemark()).isNull();
        assertThat(response.getTotalScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getQuestionCount()).isZero();
    }

    @Test
    void shouldRejectDuplicatePaperName() {
        assertThatThrownBy(() -> adminPaperService.create(saveRequest(" Java 基础试卷 ", "duplicate", 90, "duplicate")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("试卷名称已存在");
    }

    @Test
    void shouldRefreshTotalScoreFromPaperQuestions() {
        jdbcTemplate.update("update paper_question set item_score = ? where paper_id = ? and id = ?", new BigDecimal("20.00"), 1L, 2L);

        adminPaperService.refreshTotalScore(1L);

        Paper paper = paperMapper.selectById(1L);
        assertThat(paper.getTotalScore()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void shouldSoftDeletePaper() {
        adminPaperService.delete(2L);

        Paper paper = paperMapper.selectById(2L);
        assertThat(paper).isNotNull();
        assertThat(paper.getDeleted()).isEqualTo(1);
        assertThatThrownBy(() -> adminPaperService.requireActive(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("试卷不存在");
    }

    private SavePaperRequest saveRequest(String name, String description, Integer durationMinutes, String remark) {
        SavePaperRequest request = new SavePaperRequest();
        request.setName(name);
        request.setDescription(description);
        request.setDurationMinutes(durationMinutes);
        request.setRemark(remark);
        return request;
    }
}
