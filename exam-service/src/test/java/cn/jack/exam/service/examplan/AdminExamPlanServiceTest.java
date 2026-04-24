package cn.jack.exam.service.examplan;

import cn.jack.exam.dto.examplan.SaveExamPlanRequest;
import cn.jack.exam.dto.examplan.UpdateExamPlanExamineesRequest;
import cn.jack.exam.dto.examplan.UpdateExamPlanStatusRequest;
import cn.jack.exam.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminExamPlanServiceTest {

    @Autowired
    private AdminExamPlanService adminExamPlanService;

    @Test
    void shouldCreateDraftExamPlanWhenWindowIsValid() {
        SaveExamPlanRequest request = new SaveExamPlanRequest();
        request.setName("服务层考试计划");
        request.setPaperId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        request.setRemark("service-test");

        var response = adminExamPlanService.create(request);

        assertThat(response.getName()).isEqualTo("服务层考试计划");
        assertThat(response.getPaperId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("DRAFT");
        assertThat(response.getEffectiveExamineeCount()).isZero();
    }

    @Test
    void shouldRejectCreateWhenTimeWindowIsShorterThanPaperDuration() {
        SaveExamPlanRequest request = new SaveExamPlanRequest();
        request.setName("非法考试计划");
        request.setPaperId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(90));

        assertThatThrownBy(() -> adminExamPlanService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("考试时间窗口不能短于试卷时长");
    }

    @Test
    void shouldRejectReplacingExamineesWhenRequestContainsDisabledCandidate() {
        UpdateExamPlanExamineesRequest request = new UpdateExamPlanExamineesRequest();
        request.setExamineeIds(List.of(1L, 2L));

        assertThatThrownBy(() -> adminExamPlanService.replaceExaminees(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("考生不存在或不可用");
    }

    @Test
    void shouldPublishDraftPlanAfterReplacingEnabledExaminees() {
        UpdateExamPlanExamineesRequest replaceRequest = new UpdateExamPlanExamineesRequest();
        replaceRequest.setExamineeIds(List.of(1L, 3L, 1L));
        UpdateExamPlanStatusRequest statusRequest = new UpdateExamPlanStatusRequest();
        statusRequest.setStatus("published");

        var rangeResponse = adminExamPlanService.replaceExaminees(2L, replaceRequest);
        var detailResponse = adminExamPlanService.updateStatus(2L, statusRequest);

        assertThat(rangeResponse.getPlanId()).isEqualTo(2L);
        assertThat(rangeResponse.getEffectiveExamineeCount()).isEqualTo(2L);
        assertThat(detailResponse.getStatus()).isEqualTo("PUBLISHED");
        assertThat(detailResponse.getEffectiveExamineeCount()).isEqualTo(2L);
    }
}
