package cn.jack.exam.service.candidate;

import cn.jack.exam.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Service
public class CandidateTokenService {

    @Value("${exam.candidate-auth.token-secret:exam-candidate-auth-secret}")
    private String tokenSecret;

    @Value("${exam.candidate-auth.token-expire-hours:4}")
    private long tokenExpireHours;

    public CandidateIssuedToken issueToken(Long examineeId, boolean profileConfirmed) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpireHours);
        String payload = examineeId
                + ":" + profileConfirmed
                + ":" + expiresAt.toEpochSecond(ZoneOffset.UTC)
                + ":" + UUID.randomUUID();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
        return new CandidateIssuedToken(encodedPayload + "." + signature, expiresAt);
    }

    public CandidateTokenClaims parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }

        String[] segments = token.split("\\.");
        if (segments.length != 2) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }

        String payload = decode(segments[0]);
        String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
        if (!expectedSignature.equals(segments[1])) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }

        String[] fields = payload.split(":");
        if (fields.length != 4) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }

        try {
            Long examineeId = Long.parseLong(fields[0]);
            boolean profileConfirmed = Boolean.parseBoolean(fields[1]);
            LocalDateTime expiresAt = LocalDateTime.ofEpochSecond(Long.parseLong(fields[2]), 0, ZoneOffset.UTC);
            if (expiresAt.isBefore(LocalDateTime.now())) {
                throw new UnauthorizedException("考生登录已失效或不存在");
            }
            return new CandidateTokenClaims(examineeId, profileConfirmed, expiresAt);
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign candidate token", exception);
        }
    }

    private String decode(String encoded) {
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("考生登录已失效或不存在");
        }
    }

    public record CandidateIssuedToken(String token, LocalDateTime expiresAt) {
    }

    public record CandidateTokenClaims(Long examineeId, boolean profileConfirmed, LocalDateTime expiresAt) {
    }
}
