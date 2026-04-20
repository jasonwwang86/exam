package cn.jack.exam.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminMenuItemResponse {

    private final String code;
    private final String name;
    private final String path;
}
