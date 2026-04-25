package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_result")
public class ExamResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("exam_plan_id")
    private Long examPlanId;

    @TableField("examinee_id")
    private Long examineeId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("paper_id")
    private Long paperId;

    @TableField("score_status")
    private String scoreStatus;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("objective_score")
    private BigDecimal objectiveScore;

    @TableField("subjective_score")
    private BigDecimal subjectiveScore;

    @TableField("answered_count")
    private Integer answeredCount;

    @TableField("unanswered_count")
    private Integer unansweredCount;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
