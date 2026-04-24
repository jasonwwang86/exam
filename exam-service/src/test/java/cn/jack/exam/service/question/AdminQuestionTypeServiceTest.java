package cn.jack.exam.service.question;

import cn.jack.exam.dto.question.SaveQuestionTypeRequest;
import cn.jack.exam.entity.QuestionType;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.QuestionTypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminQuestionTypeServiceTest {

    @Autowired
    private AdminQuestionTypeService adminQuestionTypeService;

    @Autowired
    private QuestionTypeMapper questionTypeMapper;

    @Test
    void shouldCreateQuestionTypeWithTrimmedName() {
        var response = adminQuestionTypeService.create(saveRequest("  材料分析题  ", "TEXT", 50, "service-test"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("材料分析题");
        assertThat(response.getAnswerMode()).isEqualTo("TEXT");
    }

    @Test
    void shouldRejectDuplicateQuestionTypeName() {
        assertThatThrownBy(() -> adminQuestionTypeService.create(saveRequest(" 单选题 ", "SINGLE_CHOICE", 11, "duplicate")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("题型名称已存在");
    }

    @Test
    void shouldRejectDeletingReferencedQuestionType() {
        assertThatThrownBy(() -> adminQuestionTypeService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("题型已被题目引用，无法删除");
    }

    @Test
    void shouldSoftDeleteUnusedQuestionType() {
        var created = adminQuestionTypeService.create(saveRequest("可删除题型", "TEXT", 70, "delete"));

        adminQuestionTypeService.delete(created.getId());

        QuestionType questionType = questionTypeMapper.selectById(created.getId());
        assertThat(questionType).isNotNull();
        assertThat(questionType.getDeleted()).isEqualTo(1);
        assertThatThrownBy(() -> adminQuestionTypeService.requireActive(created.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("题型不存在");
    }

    private SaveQuestionTypeRequest saveRequest(String name, String answerMode, Integer sort, String remark) {
        SaveQuestionTypeRequest request = new SaveQuestionTypeRequest();
        request.setName(name);
        request.setAnswerMode(answerMode);
        request.setSort(sort);
        request.setRemark(remark);
        return request;
    }
}
