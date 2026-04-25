package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_result_item")
public class ExamResultItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("result_id")
    private Long resultId;

    @TableField("paper_question_id")
    private Long paperQuestionId;

    @TableField("question_id")
    private Long questionId;

    @TableField("question_no")
    private Integer questionNo;

    @TableField("question_stem_snapshot")
    private String questionStemSnapshot;

    @TableField("question_type_name_snapshot")
    private String questionTypeNameSnapshot;

    @TableField("item_score")
    private BigDecimal itemScore;

    @TableField("awarded_score")
    private BigDecimal awardedScore;

    @TableField("answer_status")
    private String answerStatus;

    @TableField("answer_summary")
    private String answerSummary;

    @TableField("judge_status")
    private String judgeStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
