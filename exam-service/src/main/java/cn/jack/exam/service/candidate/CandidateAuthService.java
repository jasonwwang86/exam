package cn.jack.exam.service.candidate;

import cn.jack.exam.config.CandidateUserContext;
import cn.jack.exam.config.TraceContext;
import cn.jack.exam.dto.candidate.CandidateAvailableExamResponse;
import cn.jack.exam.dto.candidate.CandidateConfirmResponse;
import cn.jack.exam.dto.candidate.CandidateLoginRequest;
import cn.jack.exam.dto.candidate.CandidateLoginResponse;
import cn.jack.exam.dto.candidate.CandidateProfileResponse;
import cn.jack.exam.dto.candidate.CandidateProfileSummaryResponse;
import cn.jack.exam.entity.Examinee;
import cn.jack.exam.entity.ExamResult;
import cn.jack.exam.exception.ForbiddenException;
import cn.jack.exam.exception.UnauthorizedException;
import cn.jack.exam.mapper.ExamAnswerSessionMapper;
import cn.jack.exam.mapper.ExamPlanMapper;
import cn.jack.exam.mapper.ExamResultMapper;
import cn.jack.exam.mapper.ExamineeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateAuthService {

    private final ExamineeMapper examineeMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamAnswerSessionMapper examAnswerSessionMapper;
    private final ExamResultMapper examResultMapper;
    private final CandidateTokenService candidateTokenService;
    private final CandidateAnsweringService candidateAnsweringService;

    public CandidateLoginResponse login(CandidateLoginRequest request) {
        Examinee examinee = examineeMapper.selectOne(new LambdaQueryWrapper<Examinee>()
                .eq(Examinee::getExamineeNo, request.getExamineeNo().trim())
                .eq(Examinee::getDeleted, 0)
                .last("limit 1"));
        if (examinee == null
                || !"ENABLED".equalsIgnoreCase(examinee.getStatus())
                || !request.getIdCardNo().trim().equals(examinee.getIdCardNo())) {
            log.warn("traceNo={} event=candidate_login_failed examineeNo={} reason=invalid_credentials",
                    TraceContext.getTraceNo(),
                    request.getExamineeNo().trim());
            throw new UnauthorizedException("考生编号或身份证号错误");
        }

        CandidateTokenService.CandidateIssuedToken issuedToken = candidateTokenService.issueToken(examinee.getId(), false);
        log.info("traceNo={} event=candidate_login_success candidateId={} examineeNo={}",
                TraceContext.getTraceNo(),
                examinee.getId(),
                examinee.getExamineeNo());
        return buildLoginResponse(examinee, false, issuedToken);
    }

    public CandidateUserContext loadContextByToken(String token) {
        CandidateTokenService.CandidateTokenClaims claims = candidateTokenService.parseToken(token);
        Examinee examinee = examineeMapper.selectById(claims.examineeId());
        if (examinee == null
                || examinee.getDeleted() == null
                || examinee.getDeleted() != 0
                || !"ENABLED".equalsIgnoreCase(examinee.getStatus())) {
            log.warn("traceNo={} event=candidate_authentication_failed candidateId={} reason=invalid_candidate",
                    TraceContext.getTraceNo(),
                    claims.examineeId());
            throw new UnauthorizedException("考生登录已失效或不存在");
        }

        return CandidateUserContext.builder()
                .examinee(examinee)
                .profileConfirmed(claims.profileConfirmed())
                .token(token)
                .expiresAt(claims.expiresAt())
                .build();
    }

    public CandidateProfileResponse profile(CandidateUserContext context) {
        return CandidateProfileResponse.builder()
                .examineeId(context.getExaminee().getId())
                .examineeNo(context.getExaminee().getExamineeNo())
                .name(context.getExaminee().getName())
                .maskedIdCardNo(maskIdCardNo(context.getExaminee().getIdCardNo()))
                .profileConfirmed(context.isProfileConfirmed())
                .message(context.isProfileConfirmed() ? "身份信息已确认，可查看可参加考试" : "请先确认身份信息后查看可参加考试")
                .build();
    }

    public CandidateConfirmResponse confirmProfile(CandidateUserContext context) {
        CandidateTokenService.CandidateIssuedToken issuedToken = candidateTokenService.issueToken(context.getExaminee().getId(), true);
        log.info("traceNo={} event=candidate_profile_confirmed candidateId={}",
                TraceContext.getTraceNo(),
                context.getExaminee().getId());
        return CandidateConfirmResponse.builder()
                .token(issuedToken.token())
                .tokenType("Bearer")
                .expiresAt(issuedToken.expiresAt())
                .profileConfirmed(true)
                .profile(buildProfileSummary(context.getExaminee()))
                .build();
    }

    public List<CandidateAvailableExamResponse> listAvailableExams(CandidateUserContext context) {
        if (!context.isProfileConfirmed()) {
            throw new ForbiddenException("请先确认身份信息");
        }
        List<CandidateAvailableExamResponse> exams = examPlanMapper.findAvailableForCandidate(
                context.getExaminee().getId(),
                LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        exams.forEach(exam -> applyAnsweringSummary(exam, context.getExaminee().getId(), now));
        log.info("traceNo={} event=candidate_exam_listed candidateId={} examCount={}",
                TraceContext.getTraceNo(),
                context.getExaminee().getId(),
                exams.size());
        return exams;
    }

    private CandidateLoginResponse buildLoginResponse(Examinee examinee,
                                                      boolean profileConfirmed,
                                                      CandidateTokenService.CandidateIssuedToken issuedToken) {
        return CandidateLoginResponse.builder()
                .token(issuedToken.token())
                .tokenType("Bearer")
                .expiresAt(issuedToken.expiresAt())
                .profileConfirmed(profileConfirmed)
                .profile(buildProfileSummary(examinee))
                .build();
    }

    private CandidateProfileSummaryResponse buildProfileSummary(Examinee examinee) {
        return CandidateProfileSummaryResponse.builder()
                .examineeId(examinee.getId())
                .examineeNo(examinee.getExamineeNo())
                .name(examinee.getName())
                .maskedIdCardNo(maskIdCardNo(examinee.getIdCardNo()))
                .build();
    }

    private String maskIdCardNo(String idCardNo) {
        if (idCardNo == null || idCardNo.length() <= 10) {
            return "********";
        }
        return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
    }

    private void applyAnsweringSummary(CandidateAvailableExamResponse exam,
                                       Long examineeId,
                                       LocalDateTime now) {
        var session = examAnswerSessionMapper.selectOne(new LambdaQueryWrapper<cn.jack.exam.entity.ExamAnswerSession>()
                .eq(cn.jack.exam.entity.ExamAnswerSession::getExamPlanId, exam.getPlanId())
                .eq(cn.jack.exam.entity.ExamAnswerSession::getExamineeId, examineeId)
                .last("limit 1"));

        if (session == null) {
            exam.setAnsweringStatus("NOT_STARTED");
            exam.setRemainingSeconds(null);
            exam.setCanEnterAnswering(!now.isBefore(exam.getStartTime()) && now.isBefore(exam.getEndTime()));
            applyScoreSummary(exam, examineeId);
            return;
        }

        session = candidateAnsweringService.normalizeSessionForRead(session, now);
        if (candidateAnsweringService.isFinalSubmitted(session)) {
            exam.setAnsweringStatus(session.getStatus());
            exam.setRemainingSeconds(0L);
            exam.setCanEnterAnswering(false);
            exam.setSubmittedAt(session.getSubmittedAt());
            exam.setSubmissionMethod(candidateAnsweringService.resolveSubmissionMethod(session.getStatus()));
            applyScoreSummary(exam, examineeId);
            return;
        }

        long remainingSeconds = Math.max(0, java.time.Duration.between(now, session.getDeadlineAt()).getSeconds());
        boolean timeExpired = remainingSeconds <= 0 || "TIME_EXPIRED".equals(session.getStatus());
        exam.setAnsweringStatus(timeExpired ? "TIME_EXPIRED" : session.getStatus());
        exam.setRemainingSeconds(remainingSeconds);
        exam.setCanEnterAnswering(!timeExpired && now.isBefore(exam.getEndTime()));
        applyScoreSummary(exam, examineeId);
    }

    private void applyScoreSummary(CandidateAvailableExamResponse exam, Long examineeId) {
        ExamResult result = examResultMapper.selectOne(new LambdaQueryWrapper<ExamResult>()
                .eq(ExamResult::getExamPlanId, exam.getPlanId())
                .eq(ExamResult::getExamineeId, examineeId)
                .last("limit 1"));
        if (result == null) {
            exam.setScoreStatus(null);
            exam.setReportAvailable(false);
            exam.setTotalScore(null);
            exam.setResultGeneratedAt(null);
            return;
        }

        exam.setScoreStatus(result.getScoreStatus());
        exam.setReportAvailable(true);
        exam.setTotalScore(result.getTotalScore());
        exam.setResultGeneratedAt(result.getGeneratedAt());
    }
}
