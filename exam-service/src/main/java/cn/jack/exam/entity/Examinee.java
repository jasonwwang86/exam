package cn.jack.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("examinee")
public class Examinee {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("examinee_no")
    private String examineeNo;

    private String name;

    private String gender;

    @TableField("id_card_no")
    private String idCardNo;

    private String phone;

    private String email;

    private String status;

    private String remark;

    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
