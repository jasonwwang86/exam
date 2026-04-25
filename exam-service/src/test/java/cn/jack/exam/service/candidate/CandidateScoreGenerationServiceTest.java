package cn.jack.exam.service.candidate;

import cn.jack.exam.entity.ExamAnswerSession;
import cn.jack.exam.entity.ExamResult;
import cn.jack.exam.entity.ExamResultItem;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamResultItemMapper;
import cn.jack.exam.mapper.ExamResultMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CandidateScoreGenerationServiceTest {

    @Autowired
    private CandidateScoreGenerationService candidateScoreGenerationService;

    @Autowired
    private ExamAnswerSessionMapper examAnswerSessionMapper;

    @Autowired
    private ExamResultMapper examResultMapper;

    @Autowired
    private ExamResultItemMapper examResultItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldJudgeMultipleChoiceTrueFalseAndNormalizedTextAnswers() {
        Long paperId = insertPaper("判分规则试卷");
        Long multiChoiceQuestionId = insertQuestion("多选题", 2L, new BigDecimal("4.00"),
                """
                {"options":[{"key":"A","content":"A"},{"key":"B","content":"B"},{"key":"C","content":"C"}],"correctOptions":["A","C"]}
                """);
        Long trueFalseQuestionId = insertQuestion("判断题", 3L, new BigDecimal("2.00"),
                """
                {"correctAnswer":false}
                """);
        Long textQuestionId = insertQuestion("文本题", 4L, new BigDecimal("6.00"),
                """
                {"acceptedAnswers":["Java Virtual Machine","Java虚拟机"]}
                """);

        Long paperQuestion1 = insertPaperQuestion(paperId, multiChoiceQuestionId, "多选题", new BigDecimal("4.00"), 1,
                """
                {"options":[{"key":"A","content":"A"},{"key":"B","content":"B"},{"key":"C","content":"C"}],"correctOptions":["A","C"]}
                """);
        Long paperQuestion2 = insertPaperQuestion(paperId, trueFalseQuestionId, "判断题", new BigDecimal("2.00"), 2,
                """
                {"correctAnswer":false}
                """);
        Long paperQuestion3 = insertPaperQuestion(paperId, textQuestionId, "简答题", new BigDecimal("6.00"), 3,
                """
                {"acceptedAnswers":["Java Virtual Machine","Java虚拟机"]}
                """);

        ExamAnswerSession session = insertSubmittedSession(301L, paperId, 1L);
        insertAnswerRecord(session.getId(), paperQuestion1, multiChoiceQuestionId,
                """
                {"selectedOptions":["C","A"]}
                """);
        insertAnswerRecord(session.getId(), paperQuestion2, trueFalseQuestionId,
                """
                {"selectedAnswer":false}
                """);
        insertAnswerRecord(session.getId(), paperQuestion3, textQuestionId,
                """
                {"textAnswer":"  java   virtual machine  "}
                """);

        candidateScoreGenerationService.generateForSession(session);

        ExamResult result = loadResult(301L, 1L);
        assertThat(result).isNotNull();
        assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(result.getObjectiveScore()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(result.getSubjectiveScore()).isEqualByComparingTo(new BigDecimal("6.00"));

        List<ExamResultItem> items = examResultItemMapper.selectList(new LambdaQueryWrapper<ExamResultItem>()
                .eq(ExamResultItem::getResultId, result.getId())
                .orderByAsc(ExamResultItem::getQuestionNo));
        assertThat(items).hasSize(3);
        assertThat(items).allMatch(item -> "CORRECT".equals(item.getJudgeStatus()));
        assertThat(items).extracting(ExamResultItem::getAwardedScore)
                .containsExactly(new BigDecimal("4.00"), new BigDecimal("2.00"), new BigDecimal("6.00"));
    }

    @Test
    void shouldRetryMissingScoreResultForSubmittedSession() {
        ExamAnswerSession session = insertSubmittedSession(302L, 1L, 1L);
        insertAnswerRecord(session.getId(), 1L, 1L,
                """
                {"selectedOption":"A"}
                """);

        candidateScoreGenerationService.retryPendingScoreGeneration();

        ExamResult result = loadResult(302L, 1L);
        assertThat(result).isNotNull();
        assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void shouldKeepExistingResultWhenRegenerationFails() {
        ExamAnswerSession session = insertSubmittedSession(303L, 1L, 1L);
        insertAnswerRecord(session.getId(), 1L, 1L,
                """
                {"selectedOption":"A"}
                """);

        candidateScoreGenerationService.generateForSession(session);
        ExamResult existing = loadResult(303L, 1L);
        assertThat(existing).isNotNull();

        jdbcTemplate.update("update paper_question set answer_config_snapshot = ? where id = ?",
                "{invalid-json",
                1L);

        try {
            candidateScoreGenerationService.generateForSession(session);
        } catch (RuntimeException ignored) {
        }

        ExamResult retained = loadResult(303L, 1L);
        assertThat(retained).isNotNull();
        assertThat(retained.getId()).isEqualTo(existing.getId());
        assertThat(retained.getTotalScore()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(examResultItemMapper.selectCount(new LambdaQueryWrapper<ExamResultItem>()
                .eq(ExamResultItem::getResultId, retained.getId()))).isEqualTo(2);
    }

    private Long insertPaper(String name) {
        jdbcTemplate.update(
                """
                insert into paper (name, description, duration_minutes, total_score, remark, deleted, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                name,
                name,
                60,
                BigDecimal.ZERO,
                name,
                0
        );
        return jdbcTemplate.queryForObject("select id from paper where name = ?", Long.class, name);
    }

    private Long insertQuestion(String stem, Long questionTypeId, BigDecimal score, String answerConfig) {
        jdbcTemplate.update(
                """
                insert into question (stem, question_type_id, difficulty, score, answer_config, deleted, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                stem,
                questionTypeId,
                "EASY",
                score,
                answerConfig,
                0
        );
        return jdbcTemplate.queryForObject("select id from question where stem = ?", Long.class, stem);
    }

    private Long insertPaperQuestion(Long paperId,
                                     Long questionId,
                                     String questionTypeName,
                                     BigDecimal itemScore,
                                     int displayOrder,
                                     String answerConfigSnapshot) {
        String stem = "题目-" + questionId + "-" + displayOrder;
        jdbcTemplate.update(
                """
                insert into paper_question (
                    paper_id, question_id, question_stem_snapshot, question_type_name_snapshot, difficulty_snapshot,
                    answer_config_snapshot, item_score, display_order, deleted, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                paperId,
                questionId,
                stem,
                questionTypeName,
                "EASY",
                answerConfigSnapshot,
                itemScore,
                displayOrder,
                0
        );
        return jdbcTemplate.queryForObject(
                "select id from paper_question where paper_id = ? and question_id = ? and display_order = ?",
                Long.class,
                paperId,
                questionId,
                displayOrder
        );
    }

    private ExamAnswerSession insertSubmittedSession(Long planId, Long paperId, Long examineeId) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                insert into exam_plan (id, name, paper_id, start_time, end_time, status, remark, deleted, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                planId,
                "成绩生成场次-" + planId,
                paperId,
                Timestamp.valueOf(now.minusHours(2)),
                Timestamp.valueOf(now.plusHours(1)),
                "PUBLISHED",
                "score-generation-test",
                0
        );
        jdbcTemplate.update(
                "insert into exam_plan_examinee (exam_plan_id, examinee_id, created_at) values (?, ?, current_timestamp)",
                planId,
                examineeId
        );
        jdbcTemplate.update(
                """
                insert into exam_answer_session (
                    exam_plan_id, examinee_id, paper_id, started_at, deadline_at, status, last_saved_at, submitted_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                planId,
                examineeId,
                paperId,
                Timestamp.valueOf(now.minusMinutes(30)),
                Timestamp.valueOf(now.minusMinutes(1)),
                "SUBMITTED",
                Timestamp.valueOf(now.minusMinutes(5)),
                Timestamp.valueOf(now.minusMinutes(3)),
                Timestamp.valueOf(now.minusMinutes(30)),
                Timestamp.valueOf(now.minusMinutes(3))
        );
        return examAnswerSessionMapper.selectOne(new LambdaQueryWrapper<ExamAnswerSession>()
                .eq(ExamAnswerSession::getExamPlanId, planId)
                .eq(ExamAnswerSession::getExamineeId, examineeId)
                .last("limit 1"));
    }

    private void insertAnswerRecord(Long sessionId, Long paperQuestionId, Long questionId, String answerContent) {
        jdbcTemplate.update(
                """
                insert into exam_answer_record (
                    session_id, paper_question_id, question_id, answer_content, answer_status, last_saved_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                sessionId,
                paperQuestionId,
                questionId,
                answerContent,
                "ANSWERED"
        );
    }

    private ExamResult loadResult(Long planId, Long examineeId) {
        return examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, planId)
                .eq(ExamResult::getExamineeId, examineeId)
                .last("limit 1"));
    }
}
