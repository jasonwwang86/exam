package cn.jack.exam.mapper;

import cn.jack.exam.entity.AdminPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminPermissionMapper extends BaseMapper<AdminPermission> {

    List<AdminPermission> findByUserId(@Param("userId") Long userId);
}
