package cn.jack.exam.dto.examinee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExamineeListItemResponse {

    private final Long id;
    private final String examineeNo;
    private final String name;
    private final String gender;
    private final String idCardNo;
    private final String phone;
    private final String email;
    private final String status;
    private final String remark;
    private final LocalDateTime updatedAt;
}
