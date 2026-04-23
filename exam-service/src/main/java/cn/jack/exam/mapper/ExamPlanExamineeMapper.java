package cn.jack.exam.mapper;

import cn.jack.exam.dto.examplan.ExamPlanExamineeResponse;
import cn.jack.exam.entity.ExamPlanExaminee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExamPlanExamineeMapper extends BaseMapper<ExamPlanExaminee> {

    List<ExamPlanExamineeResponse> findByPlanId(@Param("planId") Long planId);
}
