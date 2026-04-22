package cn.jack.exam.mapper;

import cn.jack.exam.dto.question.QuestionListItemResponse;
import cn.jack.exam.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    List<QuestionListItemResponse> findPage(@Param("keyword") String keyword,
                                            @Param("questionTypeId") Long questionTypeId,
                                            @Param("difficulty") String difficulty,
                                            @Param("offset") long offset,
                                            @Param("pageSize") long pageSize);

    long countPage(@Param("keyword") String keyword,
                   @Param("questionTypeId") Long questionTypeId,
                   @Param("difficulty") String difficulty);

    long countActiveByQuestionTypeId(@Param("questionTypeId") Long questionTypeId);
}
