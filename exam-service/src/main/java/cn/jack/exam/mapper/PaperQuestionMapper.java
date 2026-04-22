package cn.jack.exam.mapper;

import cn.jack.exam.dto.paper.PaperQuestionResponse;
import cn.jack.exam.entity.PaperQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {

    List<PaperQuestionResponse> findByPaperId(@Param("paperId") Long paperId);

    long countActiveByPaperId(@Param("paperId") Long paperId);

    BigDecimal sumScoreByPaperId(@Param("paperId") Long paperId);

    Integer findMaxDisplayOrder(@Param("paperId") Long paperId);
}
