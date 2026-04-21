package cn.jack.exam.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlDatasourceConfigTest {

    private static final Path APPLICATION_CONFIG_PATH =
            Path.of("src", "main", "resources", "application.yml");

    @Test
    void shouldAllowPublicKeyRetrievalForMysqlDatasource() throws IOException {
        String applicationConfig = Files.readString(APPLICATION_CONFIG_PATH);

        assertThat(applicationConfig)
                .contains("allowPublicKeyRetrieval=true");
    }
}
