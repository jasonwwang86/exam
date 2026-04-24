package cn.jack.exam.dto.candidate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CandidateLoginRequest {

    @NotBlank(message = "考生编号不能为空")
    private String examineeNo;

    @NotBlank(message = "身份证号不能为空")
    private String idCardNo;
}
