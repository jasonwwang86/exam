package cn.jack.exam.mapper;

import cn.jack.exam.entity.AdminRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    List<AdminRole> findByUserId(@Param("userId") Long userId);
}
