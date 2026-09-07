package fr.insee.pogues.mapper;

import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.model.dto.release.OverrideGenerationParameters;
import fr.insee.pogues.model.dto.release.ReleaseRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseRequestMapperTest {

    private final ReleaseRequestMapper mapper = new ReleaseRequestMapper();


    @Test
    @DisplayName("when entity is null, should return null dto")
    void should_return_null_when_entity_is_null() {
        // Given
        ReleaseRequestDB entity = null;

        // When
        ReleaseRequestDto dto = mapper.toDto(entity);

        // Then
        assertThat(dto).isNull();
    }


    @Test
    @DisplayName("should map all fields from entity to dto")
    void should_map_all_fields_when_entity_is_valid() {
        // Given
        Long id = 1L;
        String author = "author";
        Instant requestDate = Instant.parse("2026-07-27T09:33:00Z");
        ReleaseRequestStatus status = ReleaseRequestStatus.RUNNING;
        ReleaseRequestStatusCode statusDescription = ReleaseRequestStatusCode.ERROR_GENERATION_DDI;
        UUID poguesVersionId = UUID.randomUUID();
        String poguesId = "pogues-id";
        String releaseDescription = "release description";
        CollectMode mode = CollectMode.CAWI;
        GenerationContext context = GenerationContext.HOUSEHOLD;
        GenerationQuestionNumberingMode numberingMode = GenerationQuestionNumberingMode.SEQUENCE;
        Boolean responseTimeQuestion = true;

        ReleaseRequestDB entity = new ReleaseRequestDB();
        entity.setId(id);
        entity.setRequestedBy(author);
        entity.setRequestedAt(requestDate);
        entity.setStatus(status);
        entity.setStatusDescription(statusDescription);
        entity.setPoguesVersionId(poguesVersionId);
        entity.setPoguesId(poguesId);
        entity.setReleaseDescription(releaseDescription);
        entity.setContext(context);
        entity.setGenerationParamsQuestionNumberingMode(numberingMode);
        entity.setGenerationParamsResponseTimeQuestion(responseTimeQuestion);

        ReleaseRequestModeDB requestModeDB = new ReleaseRequestModeDB();
        requestModeDB.setMode(mode);

        entity.addMode(requestModeDB);

        // When
        ReleaseRequestDto dto = mapper.toDto(entity);

        // Then
        assertThat(dto).isEqualTo(new ReleaseRequestDto(
                id,
                author,
                requestDate,
                status,
                statusDescription.label,
                poguesVersionId,
                poguesId,
                releaseDescription,
                List.of(mode),
                context,
                new OverrideGenerationParameters(numberingMode, responseTimeQuestion)
        ));
    }
}