package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_answer_session")
public class ExamAnswerSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("exam_plan_id")
    private Long examPlanId;

    @TableField("examinee_id")
    private Long examineeId;

    @TableField("paper_id")
    private Long paperId;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("deadline_at")
    private LocalDateTime deadlineAt;

    private String status;

    @TableField("last_saved_at")
    private LocalDateTime lastSavedAt;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
