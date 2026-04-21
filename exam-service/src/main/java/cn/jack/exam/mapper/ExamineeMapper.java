package cn.jack.exam.mapper;

import cn.jack.exam.dto.examinee.ExamineeListItemResponse;
import cn.jack.exam.entity.Examinee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExamineeMapper extends BaseMapper<Examinee> {

    List<ExamineeListItemResponse> findPage(@Param("keyword") String keyword,
                                            @Param("status") String status,
                                            @Param("offset") long offset,
                                            @Param("pageSize") long pageSize);

    long countPage(@Param("keyword") String keyword,
                   @Param("status") String status);

    List<Examinee> findForExport(@Param("keyword") String keyword,
                                 @Param("status") String status);
}
