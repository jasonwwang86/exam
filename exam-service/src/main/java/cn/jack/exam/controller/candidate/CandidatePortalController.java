package cn.jack.exam.controller.candidate;

import cn.jack.exam.config.CandidateUserContextHolder;
import cn.jack.exam.dto.candidate.CandidateAvailableExamResponse;
import cn.jack.exam.dto.candidate.CandidateConfirmResponse;
import cn.jack.exam.dto.candidate.CandidateLoginRequest;
import cn.jack.exam.dto.candidate.CandidateLoginResponse;
import cn.jack.exam.dto.candidate.CandidateProfileResponse;
import cn.jack.exam.service.candidate.CandidateAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidatePortalController {

    private final CandidateAuthService candidateAuthService;

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
}
