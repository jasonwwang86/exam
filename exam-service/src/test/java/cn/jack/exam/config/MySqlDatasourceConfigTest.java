package cn.jack.exam.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlDatasourceConfigTest {

    private static final Path APPLICATION_CONFIG_PATH =
            Path.of("src", "main", "resources", "application.yml");
    private static final Path MYSQL_SCHEMA_PATH =
            Path.of("src", "main", "resources", "db", "mysql", "ddl", "schema.sql");

    @Test
    void shouldAllowPublicKeyRetrievalForMysqlDatasource() throws IOException {
        String applicationConfig = Files.readString(APPLICATION_CONFIG_PATH);

        assertThat(applicationConfig)
                .contains("allowPublicKeyRetrieval=true");
    }

    @Test
    void shouldStoreExamResultAnswerSummaryAsTextInMysqlSchema() throws IOException {
        String mysqlSchema = Files.readString(MYSQL_SCHEMA_PATH);

        assertThat(mysqlSchema)
                .contains("answer_summary text null")
                .doesNotContain("answer_summary json null");
    }
}
