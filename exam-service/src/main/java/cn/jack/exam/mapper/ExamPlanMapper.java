package cn.jack.exam.mapper;

import cn.jack.exam.dto.examplan.ExamPlanDetailResponse;
import cn.jack.exam.dto.examplan.ExamPlanListItemResponse;
import cn.jack.exam.dto.candidate.CandidateAvailableExamResponse;
import cn.jack.exam.entity.ExamPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExamPlanMapper extends BaseMapper<ExamPlan> {

    List<ExamPlanListItemResponse> findPage(@Param("keyword") String keyword,
                                            @Param("status") String status,
                                            @Param("offset") long offset,
                                            @Param("pageSize") long pageSize);

    long countPage(@Param("keyword") String keyword,
                   @Param("status") String status);

    ExamPlanDetailResponse findDetailById(@Param("planId") Long planId);

    long countValidExaminees(@Param("planId") Long planId);

    List<CandidateAvailableExamResponse> findAvailableForCandidate(@Param("examineeId") Long examineeId,
                                                                   @Param("now") LocalDateTime now);
}
