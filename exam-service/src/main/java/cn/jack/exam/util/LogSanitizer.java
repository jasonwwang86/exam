package cn.jack.exam.util;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public final class LogSanitizer {

    private static final String REDACTED = "***";
    private static final int MAX_BODY_LENGTH = 512;

    private LogSanitizer() {
    }

    public static String sanitizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String sanitized = text
                .replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"passwordHash\"\\s*:\\s*\"[^\"]*\"", "\"passwordHash\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"authorization\"\\s*:\\s*\"[^\"]*\"", "\"authorization\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"idCardNo\"\\s*:\\s*\"[^\"]*\"", "\"idCardNo\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"phone\"\\s*:\\s*\"[^\"]*\"", "\"phone\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"questionStemSnapshot\"\\s*:\\s*\"[^\"]*\"", "\"questionStemSnapshot\":\"" + REDACTED + "\"")
                .replaceAll("(?is)\"acceptedAnswers\"\\s*:\\s*\\[[^\\]]*\\]", "\"acceptedAnswers\":\"" + REDACTED + "\"")
                .replaceAll("(?is)\"correctOptions\"\\s*:\\s*\\[[^\\]]*\\]", "\"correctOptions\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"correctOption\"\\s*:\\s*\"[^\"]*\"", "\"correctOption\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"correctAnswer\"\\s*:\\s*(true|false|\"[^\"]*\")", "\"correctAnswer\":\"" + REDACTED + "\"")
                .replaceAll("(?i)\"content\"\\s*:\\s*\"[^\"]*\"", "\"content\":\"" + REDACTED + "\"")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer " + REDACTED);

        if (sanitized.length() > MAX_BODY_LENGTH) {
            return sanitized.substring(0, MAX_BODY_LENGTH) + "...";
        }
        return sanitized;
    }

    public static String summarizeBody(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return "";
        }

        if (!isVisibleContent(contentType)) {
            return "[content omitted]";
        }

        return sanitizeText(new String(body, StandardCharsets.UTF_8));
    }

    private static boolean isVisibleContent(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }

        MediaType mediaType = MediaType.parseMediaType(contentType);
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)
                || "text".equalsIgnoreCase(mediaType.getType());
    }
}
