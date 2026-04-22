package cn.jack.exam.service.paper;

import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.paper.PaperDetailResponse;
import cn.jack.exam.dto.paper.PaperListItemResponse;
import cn.jack.exam.dto.paper.PaperPageResponse;
import cn.jack.exam.dto.paper.SavePaperRequest;
import cn.jack.exam.entity.Paper;
import cn.jack.exam.exception.BadRequestException;
import cn.jack.exam.mapper.PaperMapper;
import cn.jack.exam.mapper.PaperQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaperService {

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;

    public PaperPageResponse list(String keyword, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.max(pageSize, 1);
        long offset = (safePage - 1) * safePageSize;
        List<PaperListItemResponse> records = paperMapper.findPage(keyword, offset, safePageSize);
        long total = paperMapper.countPage(keyword);
        return PaperPageResponse.builder()
                .page(safePage)
                .pageSize(safePageSize)
                .total(total)
                .records(records)
                .build();
    }

    public PaperDetailResponse get(Long id) {
        requireActive(id);
        return paperMapper.findDetailById(id);
    }

    public PaperDetailResponse create(SavePaperRequest request) {
        validateUniqueName(null, request.getName());

        LocalDateTime now = LocalDateTime.now();
        Paper paper = new Paper();
        paper.setName(request.getName().trim());
        paper.setDescription(trimToNull(request.getDescription()));
        paper.setDurationMinutes(request.getDurationMinutes());
        paper.setTotalScore(BigDecimal.ZERO);
        paper.setRemark(trimToNull(request.getRemark()));
        paper.setDeleted(0);
        paper.setCreatedAt(now);
        paper.setUpdatedAt(now);
        paperMapper.insert(paper);

        log.info("traceNo={} event=paper_created paperId={} durationMinutes={}",
                TraceContext.getTraceNo(),
                paper.getId(),
                paper.getDurationMinutes());

        return paperMapper.findDetailById(paper.getId());
    }

    public PaperDetailResponse update(Long id, SavePaperRequest request) {
        Paper paper = requireActive(id);
        validateUniqueName(id, request.getName());

        paper.setName(request.getName().trim());
        paper.setDescription(trimToNull(request.getDescription()));
        paper.setDurationMinutes(request.getDurationMinutes());
        paper.setRemark(trimToNull(request.getRemark()));
        paper.setUpdatedAt(LocalDateTime.now());
        paperMapper.updateById(paper);

        log.info("traceNo={} event=paper_updated paperId={} durationMinutes={}",
                TraceContext.getTraceNo(),
                paper.getId(),
                paper.getDurationMinutes());

        return paperMapper.findDetailById(paper.getId());
    }

    public void delete(Long id) {
        Paper paper = requireActive(id);
        paper.setDeleted(1);
        paper.setUpdatedAt(LocalDateTime.now());
        paperMapper.updateById(paper);

        log.info("traceNo={} event=paper_deleted paperId={}",
                TraceContext.getTraceNo(),
                paper.getId());
    }

    public Paper requireActive(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null || paper.getDeleted() == null || paper.getDeleted() != 0) {
            throw new BadRequestException("试卷不存在");
        }
        return paper;
    }

    public void refreshTotalScore(Long paperId) {
        Paper paper = requireActive(paperId);
        BigDecimal totalScore = paperQuestionMapper.sumScoreByPaperId(paperId);
        paper.setTotalScore(totalScore == null ? BigDecimal.ZERO : totalScore);
        paper.setUpdatedAt(LocalDateTime.now());
        paperMapper.updateById(paper);
    }

    private void validateUniqueName(Long currentId, String name) {
        Paper existing = paperMapper.selectOne(new LambdaQueryWrapper<Paper>()
                .eq(Paper::getName, name.trim())
                .eq(Paper::getDeleted, 0)
                .last("limit 1"));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BadRequestException("试卷名称已存在");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
