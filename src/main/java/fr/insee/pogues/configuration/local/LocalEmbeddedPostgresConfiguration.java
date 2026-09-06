package fr.insee.pogues.configuration.local;

import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts a real Postgres process (no Docker) for the {@code local} profile.
 * Data lives in {@code .local-postgres/} so the cluster is reused across restarts.
 */
@Configuration
@Profile("local")
@Slf4j
public class LocalEmbeddedPostgresConfiguration {

    static final String DATA_DIRECTORY = ".local-postgres";
    static final int PORT = 5433;

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        Path dataDir = Path.of(DATA_DIRECTORY).toAbsolutePath();
        Files.createDirectories(dataDir);
        log.info("Starting embedded Postgres in {} on port {}", dataDir, PORT);
        return EmbeddedPostgres.builder()
                .setPort(PORT)
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .setRegisterShutdownHook(false)
                .start();
    }

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:" + embeddedPostgres.getPort() + "/postgres");
        dataSource.setUsername("postgres");
        dataSource.setPassword("");
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }
}
