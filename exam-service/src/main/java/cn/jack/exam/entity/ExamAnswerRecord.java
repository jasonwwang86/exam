package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_answer_record")
public class ExamAnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("paper_question_id")
    private Long paperQuestionId;

    @TableField("question_id")
    private Long questionId;

    @TableField("answer_content")
    private String answerContent;

    @TableField("answer_status")
    private String answerStatus;

    @TableField("last_saved_at")
    private LocalDateTime lastSavedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
