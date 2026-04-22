package cn.jack.exam.mapper;

import cn.jack.exam.dto.paper.PaperDetailResponse;
import cn.jack.exam.dto.paper.PaperListItemResponse;
import cn.jack.exam.entity.Paper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {

    List<PaperListItemResponse> findPage(@Param("keyword") String keyword,
                                         @Param("offset") long offset,
                                         @Param("pageSize") long pageSize);

    long countPage(@Param("keyword") String keyword);

    PaperDetailResponse findDetailById(@Param("paperId") Long paperId);
}
