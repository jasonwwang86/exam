package cn.jack.exam.dto.examinee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateExamineeRequest {

    @NotBlank(message = "考生编号不能为空")
    @Size(max = 64, message = "考生编号长度不能超过64")
    private String examineeNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "MALE|FEMALE", message = "性别取值不合法")
    private String gender;

    @NotBlank(message = "身份证号不能为空")
    @Size(max = 32, message = "身份证号长度不能超过32")
    private String idCardNo;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 32, message = "手机号长度不能超过32")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态取值不合法")
    private String status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
