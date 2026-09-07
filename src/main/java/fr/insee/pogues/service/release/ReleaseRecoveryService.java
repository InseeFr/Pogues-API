package fr.insee.pogues.service.release;

import fr.insee.pogues.client.surveyregistry.questionnaire.QuestionnaireRegistryClient;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentMetadataDto;
import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.mapper.ReleaseRequestMapper;
import fr.insee.pogues.model.dto.release.CollectionInstrumentReleaseDto;
import fr.insee.pogues.model.dto.release.OverrideGenerationParameters;
import fr.insee.pogues.model.dto.release.ReleaseDto;
import fr.insee.pogues.model.dto.release.ReleaseRequestDto;
import fr.insee.pogues.persistence.repository.jpa.ReleaseRequestRepository;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.service.visualize.VisualizeUriBuilder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ReleaseRecoveryService {

    private final ReleaseRequestRepository releaseRequestRepository;
    private final QuestionnaireRegistryClient registryClient;
    private final VisualizeUriBuilder visualizeUriBuilder;
    private final ReleaseRequestMapper mapper;
    private final IQuestionnaireService questionnaireService;

    public List<ReleaseRequestDto> getPendingReleaseRequestsByPoguesId(String poguesId) {
        questionnaireService.ensureExistsById(poguesId);
        List<ReleaseRequestDB> pendingRequests = getPendingReleaseRequestsByPoguesIdInternal(poguesId);
        return pendingRequests.stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Retrieves pending release requests (not completed) for a given Pogues ID
     */
    @Transactional(readOnly = true)
    public List<ReleaseRequestDB> getPendingReleaseRequestsByPoguesIdInternal(String poguesId) {
        return releaseRequestRepository.findWithModesByPoguesIdAndStatusNot(poguesId, ReleaseRequestStatus.COMPLETED);
    }


    public List<ReleaseDto> getCompletedReleasesByPoguesId(String poguesId) {
        questionnaireService.ensureExistsById(poguesId);
        List<CollectionInstrumentMetadataDto> collectionInstruments =
                registryClient.getCollectionInstrumentByPoguesId(poguesId, true);
        List<ReleaseRequestDB> completedReleaseRequests =
                releaseRequestRepository.findWithModesByPoguesIdAndStatus(poguesId, ReleaseRequestStatus.COMPLETED);

        return completedReleaseRequests.stream()
                .map(requestDB -> toReleaseDto(requestDB, collectionInstruments))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<ReleaseDto> toReleaseDto(ReleaseRequestDB requestDB,
                                              List<CollectionInstrumentMetadataDto> collectionInstruments) {
        List<CollectionInstrumentReleaseDto> releasedInstruments = requestDB.getModes().stream()
                .map(requestModeDB -> findByPoguesVersionIdAndMode(collectionInstruments, requestDB.getPoguesVersionId(), requestModeDB.getMode()))
                .flatMap(Optional::stream)
                .map(collectionInstrument -> toCollectionInstrumentReleaseDto(requestDB, collectionInstrument))
                .toList();

        if (releasedInstruments.isEmpty()) {
            return Optional.empty();
        }

        Instant latestReleaseDate = findLatestReleaseDate(requestDB, collectionInstruments);

        return Optional.of(new ReleaseDto(
                requestDB.getRequestedBy(),
                latestReleaseDate,
                requestDB.getPoguesVersionId(),
                requestDB.getReleaseDescription(),
                requestDB.getContext(),
                releasedInstruments));
    }

    private CollectionInstrumentReleaseDto toCollectionInstrumentReleaseDto(ReleaseRequestDB requestDB,
                                                                            CollectionInstrumentMetadataDto collectionInstrument) {
        return new CollectionInstrumentReleaseDto(
                collectionInstrument.mode(),
                collectionInstrument.collectionInstrumentId(),
                collectionInstrument.version(),
                buildOverrideGenerationParameters(requestDB, collectionInstrument),
                visualizeUriBuilder.buildVisualizeUriFromCollectionInstrument(collectionInstrument));
    }


    private OverrideGenerationParameters buildOverrideGenerationParameters(ReleaseRequestDB requestDB,
                                                                           CollectionInstrumentMetadataDto collectionInstrument) {
        boolean isBusinessCawi = GenerationContext.BUSINESS.equals(requestDB.getContext())
                && CollectMode.CAWI.equals(collectionInstrument.mode());

        return isBusinessCawi
                ? new OverrideGenerationParameters(
                requestDB.getGenerationParamsQuestionNumberingMode(),
                requestDB.getGenerationParamsResponseTimeQuestion())
                : null;
    }

    private Instant findLatestReleaseDate(ReleaseRequestDB requestDB,
                                          List<CollectionInstrumentMetadataDto> collectionInstruments) {
        return requestDB.getModes().stream()
                .map(requestModeDB -> findByPoguesVersionIdAndMode(collectionInstruments, requestDB.getPoguesVersionId(), requestModeDB.getMode()))
                .flatMap(Optional::stream)
                .map(CollectionInstrumentMetadataDto::releaseDate)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private Optional<CollectionInstrumentMetadataDto> findByPoguesVersionIdAndMode(List<CollectionInstrumentMetadataDto> collectionInstruments,
                                                                                   UUID poguesVersionId, CollectMode mode) {
        return collectionInstruments.stream()
                .filter(collectionInstrument ->
                        mode.equals(collectionInstrument.mode())
                                && poguesVersionId.equals(collectionInstrument.poguesVersionId()))
                .findFirst();
    }
}
