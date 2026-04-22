package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("paper_question")
public class PaperQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("paper_id")
    private Long paperId;

    @TableField("question_id")
    private Long questionId;

    @TableField("question_stem_snapshot")
    private String questionStemSnapshot;

    @TableField("question_type_name_snapshot")
    private String questionTypeNameSnapshot;

    @TableField("difficulty_snapshot")
    private String difficultySnapshot;

    @TableField("item_score")
    private BigDecimal itemScore;

    @TableField("display_order")
    private Integer displayOrder;

    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
