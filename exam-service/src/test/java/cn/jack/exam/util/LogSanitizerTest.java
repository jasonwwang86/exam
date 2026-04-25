package cn.jack.exam.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void shouldMaskAnswerSummaryAndTextAnswerFields() {
        String sanitized = LogSanitizer.sanitizeText("""
                {
                  "textAnswer": "Java Virtual Machine",
                  "answerSummary": "Java Virtual Machine"
                }
                """);

        assertThat(sanitized).contains("\"textAnswer\":\"***\"");
        assertThat(sanitized).contains("\"answerSummary\":\"***\"");
        assertThat(sanitized).doesNotContain("Java Virtual Machine");
    }
}
