package cn.jack.exam.controller.candidate;

import cn.jack.exam.config.CandidateUserContextHolder;
import cn.jack.exam.dto.candidate.CandidateAvailableExamResponse;
import cn.jack.exam.dto.candidate.CandidateAnswerSessionResponse;
import cn.jack.exam.dto.candidate.CandidateConfirmResponse;
import cn.jack.exam.dto.candidate.CandidateLoginRequest;
import cn.jack.exam.dto.candidate.CandidateLoginResponse;
import cn.jack.exam.dto.candidate.CandidateProfileResponse;
import cn.jack.exam.dto.candidate.CandidateSaveAnswerRequest;
import cn.jack.exam.dto.candidate.CandidateSaveAnswerResponse;
import cn.jack.exam.service.candidate.CandidateAuthService;
import cn.jack.exam.service.candidate.CandidateAnsweringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidatePortalController {

    private final CandidateAuthService candidateAuthService;
    private final CandidateAnsweringService candidateAnsweringService;

    @PostMapping("/auth/login")
    public CandidateLoginResponse login(@Valid @RequestBody CandidateLoginRequest request) {
        return candidateAuthService.login(request);
    }

    @GetMapping("/profile")
    public CandidateProfileResponse profile() {
        return candidateAuthService.profile(CandidateUserContextHolder.getRequired());
    }

    @PostMapping("/profile/confirm")
    public CandidateConfirmResponse confirmProfile() {
        return candidateAuthService.confirmProfile(CandidateUserContextHolder.getRequired());
    }

    @GetMapping("/exams")
    public List<CandidateAvailableExamResponse> listAvailableExams() {
        return candidateAuthService.listAvailableExams(CandidateUserContextHolder.getRequired());
    }

    @PutMapping("/exams/{planId}/answer-session")
    public CandidateAnswerSessionResponse loadAnswerSession(@PathVariable Long planId) {
        return candidateAnsweringService.loadAnswerSession(planId, CandidateUserContextHolder.getRequired());
    }

    @PutMapping("/exams/{planId}/questions/{paperQuestionId}/answer")
    public CandidateSaveAnswerResponse saveAnswer(@PathVariable Long planId,
                                                  @PathVariable Long paperQuestionId,
                                                  @RequestBody CandidateSaveAnswerRequest request) {
        return candidateAnsweringService.saveAnswer(planId, paperQuestionId, request, CandidateUserContextHolder.getRequired());
    }
}
