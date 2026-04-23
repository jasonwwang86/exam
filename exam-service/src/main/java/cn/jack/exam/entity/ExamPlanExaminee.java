package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_plan_examinee")
public class ExamPlanExaminee {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("exam_plan_id")
    private Long examPlanId;

    @TableField("examinee_id")
    private Long examineeId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
