package cn.jack.exam.service.question;

import cn.jack.exam.dto.question.SaveQuestionRequest;
import cn.jack.exam.entity.Question;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.QuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminQuestionServiceTest {

    @Autowired
    private AdminQuestionService adminQuestionService;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateSingleChoiceQuestionWithTrimmedStem() throws Exception {
        SaveQuestionRequest request = new SaveQuestionRequest();
        request.setStem("  Spring Boot 默认端口是什么？  ");
        request.setQuestionTypeId(1L);
        request.setDifficulty("EASY");
        request.setScore(new BigDecimal("3.00"));
        request.setAnswerConfig(objectMapper.readTree("""
                {
                  "options": [
                    {"key": "A", "content": "8080"},
                    {"key": "B", "content": "80"}
                  ],
                  "correctOption": "A"
                }
                """));

        var response = adminQuestionService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStem()).isEqualTo("Spring Boot 默认端口是什么？");
        assertThat(response.getQuestionTypeId()).isEqualTo(1L);
        assertThat(response.getQuestionTypeName()).isEqualTo("单选题");
        assertThat(response.getAnswerConfig().get("correctOption").asText()).isEqualTo("A");
    }

    @Test
    void shouldRejectQuestionWhenCorrectOptionIsNotInOptions() throws Exception {
        SaveQuestionRequest request = new SaveQuestionRequest();
        request.setStem("非法单选题");
        request.setQuestionTypeId(1L);
        request.setDifficulty("EASY");
        request.setScore(new BigDecimal("2.00"));
        request.setAnswerConfig(objectMapper.readTree("""
                {
                  "options": [
                    {"key": "A", "content": "选项A"},
                    {"key": "B", "content": "选项B"}
                  ],
                  "correctOption": "C"
                }
                """));

        assertThatThrownBy(() -> adminQuestionService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("答案配置与题型不匹配");
    }

    @Test
    void shouldUpdateQuestionAndPersistNewAnswerConfig() throws Exception {
        SaveQuestionRequest request = new SaveQuestionRequest();
        request.setStem("  JVM 的英文全称是什么？ ");
        request.setQuestionTypeId(4L);
        request.setDifficulty("MEDIUM");
        request.setScore(new BigDecimal("8.00"));
        request.setAnswerConfig(objectMapper.readTree("""
                {
                  "acceptedAnswers": ["Java Virtual Machine", "Java虚拟机"]
                }
                """));

        var response = adminQuestionService.update(2L, request);

        assertThat(response.getStem()).isEqualTo("JVM 的英文全称是什么？");
        assertThat(response.getScore()).isEqualByComparingTo("8.00");
        assertThat(response.getAnswerMode()).isEqualTo("TEXT");
        assertThat(response.getAnswerConfig().get("acceptedAnswers")).hasSize(2);
    }

    @Test
    void shouldSoftDeleteQuestion() {
        adminQuestionService.delete(3L);

        Question question = questionMapper.selectById(3L);
        assertThat(question).isNotNull();
        assertThat(question.getDeleted()).isEqualTo(1);
        assertThatThrownBy(() -> adminQuestionService.get(3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("题目不存在");
    }
}
