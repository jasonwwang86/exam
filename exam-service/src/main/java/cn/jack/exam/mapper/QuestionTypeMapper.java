package cn.jack.exam.mapper;

import cn.jack.exam.dto.question.QuestionTypeResponse;
import cn.jack.exam.entity.QuestionType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuestionTypeMapper extends BaseMapper<QuestionType> {

    List<QuestionTypeResponse> findAllActive();
}
