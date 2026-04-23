package cn.jack.exam.service.examplan;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.examplan.ExamPlanDetailResponse;
import cn.jack.exam.dto.examplan.ExamPlanExamineeResponse;
import cn.jack.exam.dto.examplan.ExamPlanPageResponse;
import cn.jack.exam.dto.examplan.SaveExamPlanRequest;
import cn.jack.exam.dto.examplan.UpdateExamPlanExamineesRequest;
import cn.jack.exam.dto.examplan.UpdateExamPlanExamineesResponse;
import cn.jack.exam.dto.examplan.UpdateExamPlanStatusRequest;
import cn.jack.exam.entity.ExamPlan;
import cn.jack.exam.entity.ExamPlanExaminee;
import cn.jack.exam.entity.Examinee;
import cn.jack.exam.entity.Paper;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.ExamPlanExamineeMapper;
import cn.jack.exam.mapper.ExamPlanMapper;
import cn.jack.exam.mapper.ExamineeMapper;
import cn.jack.exam.service.paper.AdminPaperService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExamPlanService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ExamPlanMapper examPlanMapper;
    private final ExamPlanExamineeMapper examPlanExamineeMapper;
    private final ExamineeMapper examineeMapper;
    private final AdminPaperService adminPaperService;

    public ExamPlanPageResponse list(String keyword, String status, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safePage - 1) * safePageSize;
        String normalizedStatus = normalizeStatus(status, false);
        return ExamPlanPageResponse.builder()
                .page(safePage)
                .pageSize(safePageSize)
                .total(examPlanMapper.countPage(keyword, normalizedStatus))
                .records(examPlanMapper.findPage(keyword, normalizedStatus, offset, safePageSize))
                .build();
    }

    public ExamPlanDetailResponse get(Long id) {
        requireActive(id);
        return requireDetail(id);
    }

    public ExamPlanDetailResponse create(SaveExamPlanRequest request) {
        Paper paper = adminPaperService.requireActive(request.getPaperId());
        validateWindow(request.getStartTime(), request.getEndTime(), paper.getDurationMinutes());

        LocalDateTime now = LocalDateTime.now();
        ExamPlan examPlan = new ExamPlan();
        examPlan.setName(request.getName().trim());
        examPlan.setPaperId(request.getPaperId());
        examPlan.setStartTime(request.getStartTime());
        examPlan.setEndTime(request.getEndTime());
        examPlan.setStatus(STATUS_DRAFT);
        examPlan.setRemark(trimToNull(request.getRemark()));
        examPlan.setDeleted(0);
        examPlan.setCreatedAt(now);
        examPlan.setUpdatedAt(now);
        examPlanMapper.insert(examPlan);

        log.info("traceNo={} event=exam_plan_created planId={} paperId={} status={}",
                TraceContext.getTraceNo(),
                examPlan.getId(),
                examPlan.getPaperId(),
                examPlan.getStatus());

        return requireDetail(examPlan.getId());
    }

    public ExamPlanDetailResponse update(Long id, SaveExamPlanRequest request) {
        ExamPlan examPlan = requireActive(id);
        ensureEditable(examPlan);

        Paper paper = adminPaperService.requireActive(request.getPaperId());
        validateWindow(request.getStartTime(), request.getEndTime(), paper.getDurationMinutes());

        examPlan.setName(request.getName().trim());
        examPlan.setPaperId(request.getPaperId());
        examPlan.setStartTime(request.getStartTime());
        examPlan.setEndTime(request.getEndTime());
        examPlan.setRemark(trimToNull(request.getRemark()));
        examPlan.setUpdatedAt(LocalDateTime.now());
        examPlanMapper.updateById(examPlan);

        log.info("traceNo={} event=exam_plan_updated planId={} paperId={} status={}",
                TraceContext.getTraceNo(),
                examPlan.getId(),
                examPlan.getPaperId(),
                examPlan.getStatus());

        return requireDetail(id);
    }

    public UpdateExamPlanExamineesResponse replaceExaminees(Long id, UpdateExamPlanExamineesRequest request) {
        ExamPlan examPlan = requireActive(id);
        ensureEditable(examPlan);

        Set<Long> uniqueIds = new LinkedHashSet<>(request.getExamineeIds());
        for (Long examineeId : uniqueIds) {
            requireEnabledExaminee(examineeId);
        }

        examPlanExamineeMapper.delete(new LambdaQueryWrapper<ExamPlanExaminee>()
                .eq(ExamPlanExaminee::getExamPlanId, id));

        LocalDateTime now = LocalDateTime.now();
        for (Long examineeId : uniqueIds) {
            ExamPlanExaminee relation = new ExamPlanExaminee();
            relation.setExamPlanId(id);
            relation.setExamineeId(examineeId);
            relation.setCreatedAt(now);
            examPlanExamineeMapper.insert(relation);
        }

        examPlan.setUpdatedAt(now);
        examPlanMapper.updateById(examPlan);

        long effectiveCount = examPlanMapper.countValidExaminees(id);
        log.info("traceNo={} event=exam_plan_examinees_replaced planId={} examineeCount={}",
                TraceContext.getTraceNo(),
                id,
                effectiveCount);

        return UpdateExamPlanExamineesResponse.builder()
                .planId(id)
                .effectiveExamineeCount(effectiveCount)
                .build();
    }

    public List<ExamPlanExamineeResponse> listExaminees(Long id) {
        requireActive(id);
        return examPlanExamineeMapper.findByPlanId(id);
    }

    public ExamPlanDetailResponse updateStatus(Long id, UpdateExamPlanStatusRequest request) {
        ExamPlan examPlan = requireActive(id);
        String currentStatus = normalizeStatus(examPlan.getStatus(), true);
        String nextStatus = normalizeStatus(request.getStatus(), true);

        if (!currentStatus.equals(nextStatus)) {
            validateStatusTransition(currentStatus, nextStatus);
            if (STATUS_PUBLISHED.equals(nextStatus)) {
                validatePublishable(examPlan);
            }

            examPlan.setStatus(nextStatus);
            examPlan.setUpdatedAt(LocalDateTime.now());
            examPlanMapper.updateById(examPlan);

            log.info("traceNo={} event=exam_plan_status_updated planId={} fromStatus={} toStatus={} effectiveExamineeCount={}",
                    TraceContext.getTraceNo(),
                    id,
                    currentStatus,
                    nextStatus,
                    examPlanMapper.countValidExaminees(id));
        }

        return requireDetail(id);
    }

    public ExamPlan requireActive(Long id) {
        ExamPlan examPlan = examPlanMapper.selectById(id);
        if (examPlan == null || examPlan.getDeleted() == null || examPlan.getDeleted() != 0) {
            throw new BadRequestException("考试计划不存在");
        }
        return examPlan;
    }

    private ExamPlanDetailResponse requireDetail(Long id) {
        ExamPlanDetailResponse detail = examPlanMapper.findDetailById(id);
        if (detail == null) {
            throw new BadRequestException("考试计划不存在");
        }
        return detail;
    }

    private void validatePublishable(ExamPlan examPlan) {
        Paper paper = adminPaperService.requireActive(examPlan.getPaperId());
        validateWindow(examPlan.getStartTime(), examPlan.getEndTime(), paper.getDurationMinutes());
        if (examPlanMapper.countValidExaminees(examPlan.getId()) <= 0) {
            throw new BadRequestException("考试范围不能为空");
        }
    }

    private void validateWindow(LocalDateTime startTime, LocalDateTime endTime, Integer durationMinutes) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("开始时间必须早于结束时间");
        }
        if (Duration.between(startTime, endTime).toMinutes() < durationMinutes) {
            throw new BadRequestException("考试时间窗口不能短于试卷时长");
        }
    }

    private void validateStatusTransition(String currentStatus, String nextStatus) {
        if (STATUS_CLOSED.equals(currentStatus) || STATUS_CANCELLED.equals(currentStatus)) {
            throw new BadRequestException("当前考试计划状态不允许变更");
        }
        if (STATUS_DRAFT.equals(currentStatus)
                && (STATUS_PUBLISHED.equals(nextStatus) || STATUS_CANCELLED.equals(nextStatus))) {
            return;
        }
        if (STATUS_PUBLISHED.equals(currentStatus)
                && (STATUS_CLOSED.equals(nextStatus) || STATUS_CANCELLED.equals(nextStatus))) {
            return;
        }
        throw new BadRequestException("当前考试计划状态不允许变更");
    }

    private void ensureEditable(ExamPlan examPlan) {
        String status = normalizeStatus(examPlan.getStatus(), true);
        if (STATUS_CLOSED.equals(status) || STATUS_CANCELLED.equals(status)) {
            throw new BadRequestException("当前考试计划状态不允许编辑");
        }
    }

    private Examinee requireEnabledExaminee(Long id) {
        Examinee examinee = examineeMapper.selectById(id);
        if (examinee == null
                || examinee.getDeleted() == null
                || examinee.getDeleted() != 0
                || !"ENABLED".equalsIgnoreCase(examinee.getStatus())) {
            throw new BadRequestException("考生不存在或不可用");
        }
        return examinee;
    }

    private String normalizeStatus(String status, boolean strict) {
        if (status == null || status.isBlank()) {
            if (strict) {
                throw new BadRequestException("考试状态不合法");
            }
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (STATUS_DRAFT.equals(normalized)
                || STATUS_PUBLISHED.equals(normalized)
                || STATUS_CLOSED.equals(normalized)
                || STATUS_CANCELLED.equals(normalized)) {
            return normalized;
        }
        throw new BadRequestException("考试状态不合法");
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
