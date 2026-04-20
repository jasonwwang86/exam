package cn.jack.exam.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoggingPatternConfigTest {

    private static final String EXPECTED_CONSOLE_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{0} : %msg%n";

    @Autowired
    private Environment environment;

    @Test
    void shouldUseCompactConsoleLoggingPattern() {
        assertThat(environment.getProperty("logging.pattern.console"))
                .isEqualTo(EXPECTED_CONSOLE_PATTERN);
    }
}
