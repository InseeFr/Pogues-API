package fr.insee.pogues.configuration.local;

import fr.insee.pogues.configuration.properties.AnonymousUserProperties;
import fr.insee.pogues.configuration.properties.LocalProperties;
import fr.insee.pogues.persistence.repository.QuestionnaireRepository;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Locale;
import java.util.stream.Stream;

import static fr.insee.pogues.utils.json.JSONFunctions.jsonStringtoJsonNode;

/**
 * Loads Pogues JSON files dropped in {@code local-questionnaires/}.
 * The folder is the source of truth: on startup and on file change, files overwrite
 * questionnaires with the same id. Logged at INFO so a replace is never silent.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalQuestionnaireLoader implements CommandLineRunner {

    private static final String QUESTIONNAIRE_ID_PATTERN = "[a-zA-Z0-9]+";
    private static final long FILE_SETTLE_MS = 400;

    private final LocalProperties localProperties;
    private final AnonymousUserProperties anonymousUser;
    private final QuestionnaireRepository questionnaireRepository;
    private final IQuestionnaireService questionnaireService;

    private volatile WatchService watchService;

    @Override
    public void run(String... args) throws IOException {
        Path directory = resolveDirectory();
        Files.createDirectories(directory);
        loadAll(directory);
        startWatching(directory);
        log.info(
                "Local questionnaires: files in {} overwrite the same id in the database (startup and drop)",
                directory.toAbsolutePath());
    }

    private Path resolveDirectory() {
        String configured = localProperties.questionnairesDirectory();
        if (configured == null || configured.isBlank()) {
            configured = "local-questionnaires";
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private void loadAll(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(this::isJsonFile).forEach(this::loadFile);
        } catch (IOException e) {
            log.error("Failed to scan {}", directory, e);
        }
    }

    private void startWatching(Path directory) throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        Thread watcher = new Thread(() -> watchLoop(directory), "local-questionnaires-watch");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void watchLoop(Path directory) {
        try {
            while (watchService != null) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path filename = (Path) event.context();
                    Path file = directory.resolve(filename);
                    Thread.sleep(FILE_SETTLE_MS);
                    if (!isJsonFile(file)) {
                        continue;
                    }
                    loadFile(file);
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isJsonFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isRegularFile(file) && name.endsWith(".json");
    }

    private void loadFile(Path file) {
        String filename = file.getFileName().toString();
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            JsonNode node = jsonStringtoJsonNode(raw);
            if (!(node instanceof ObjectNode questionnaire)) {
                log.warn("Skip {}: root must be a JSON object", filename);
                return;
            }
            JsonNode idNode = questionnaire.get("id");
            if (idNode == null || idNode.asString() == null || idNode.asString().isBlank()) {
                log.warn("Skip {}: missing id", filename);
                return;
            }
            String id = idNode.asString();
            if (!id.matches(QUESTIONNAIRE_ID_PATTERN)) {
                log.warn("Skip {}: id '{}' must be alphanumeric (no hyphen)", filename, id);
                return;
            }
            questionnaire.put("owner", anonymousUser.stamp());
            if (questionnaire.get("lastUpdatedDate") == null
                    || questionnaire.get("lastUpdatedDate").asString().isBlank()) {
                questionnaire.put("lastUpdatedDate", Instant.now().toString());
            }
            boolean existed = questionnaireRepository.getQuestionnaireByID(id) != null;
            if (existed) {
                questionnaireService.updateQuestionnaire(id, questionnaire);
                log.info("Replaced questionnaire {} from {} (overwrote database)", id, filename);
            } else {
                questionnaireService.createQuestionnaire(questionnaire);
                log.info("Loaded questionnaire {} from {}", id, filename);
            }
        } catch (Exception e) {
            log.warn("Skip {}: {}", filename, e.getMessage());
        }
    }

    @PreDestroy
    void stopWatching() throws IOException {
        if (watchService != null) {
            watchService.close();
        }
    }
}
