package fr.insee.pogues.service.release;

import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.release.ReleaseRequestNotFoundException;
import fr.insee.pogues.exception.release.ReleaseRequestAlreadyExists;
import fr.insee.pogues.exception.release.ReleaseRequestStatusException;
import fr.insee.pogues.mapper.ReleaseRequestMapper;
import fr.insee.pogues.model.dto.release.*;
import fr.insee.pogues.persistence.repository.jpa.ReleaseRequestRepository;
import fr.insee.pogues.persistence.service.VersionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ReleasePublicationService {

    private final VersionService versionService;
    private final ReleaseRequestMapper mapper;
    private final ReleaseRequestRepository releaseRequestRepository;
    private final Clock clock;

    /**
     * Initializes a release request by orchestrating the creation and launching the async workflow
     */
    public ReleaseRequestDto initReleaseRequest(CreateReleaseRequestDto createReleaseDto, String author) {

        UUID poguesVersionId = versionService
                .getLastVersionByQuestionnaireId(createReleaseDto.poguesId(), false)
                .getId();

        log.info("Request release with params request:{}, poguesVersionId:{}, author:{}",
                createReleaseDto, poguesVersionId, author);

        ReleaseRequestDB releaseRequestDB = createReleaseRequest(
                createReleaseDto,
                poguesVersionId,
                author);
        return mapper.toDto(releaseRequestDB);
    }


    /**
     * Creates a new release request with the provided parameters
     */
    public ReleaseRequestDB createReleaseRequest(
            CreateReleaseRequestDto createReleaseRequestDto,
            UUID poguesVersionId,
            String author) {

        if (releaseRequestRepository.existsByPoguesVersionId(poguesVersionId)) {
            throw new ReleaseRequestAlreadyExists(
                    String.format("Release request for poguesVersionId: %s already exists", poguesVersionId));
        }

        ReleaseRequestDB requestDB = buildReleaseRequest(createReleaseRequestDto, poguesVersionId, author);
        createReleaseRequestDto.modes()
                .forEach(mode -> requestDB.addMode(ReleaseModeService.initModeRequest(mode)));

        return releaseRequestRepository.save(requestDB);
    }

    private ReleaseRequestDB buildReleaseRequest(
            CreateReleaseRequestDto dto, UUID poguesVersionId, String author) {

        ReleaseRequestDB requestDB = new ReleaseRequestDB();
        requestDB.setRequestedAt(ZonedDateTime.now(clock).toInstant());
        requestDB.setPoguesId(dto.poguesId());
        requestDB.setPoguesVersionId(poguesVersionId);
        requestDB.setRequestedBy(author);
        requestDB.setReleaseDescription(dto.releaseDescription());
        requestDB.setContext(dto.context());
        applyGenerationParameters(requestDB, dto.overrideGenerationParameters());
        requestDB.setCurrentStep(ReleaseRequestStep.INIT);
        requestDB.setStatus(ReleaseRequestStatus.CREATED);
        requestDB.setStatusDescription(ReleaseRequestStatusCode.NOT_STARTED);
        return requestDB;
    }

    private void applyGenerationParameters(ReleaseRequestDB requestDB, OverrideGenerationParameters parameters) {
        if (parameters == null) {
            return;
        }
        requestDB.setGenerationParamsQuestionNumberingMode(parameters.questionNumberingMode());
        requestDB.setGenerationParamsResponseTimeQuestion(parameters.responseTimeQuestion());
    }


    /**
     * Retrieves a release request by its ID
     */
    public ReleaseRequestDB getReleaseRequestById(Long releaseRequestId) {
        return releaseRequestRepository
                .findById(releaseRequestId)
                .orElseThrow(() -> new ReleaseRequestNotFoundException(releaseRequestId));
    }

    /**
     * Updates the current step of a release request
     */
    public void updateStep(Long releaseRequestId, ReleaseRequestStep currentStep) {
        ReleaseRequestDB releaseRequest = getReleaseRequestById(releaseRequestId);

        releaseRequest.setCurrentStep(currentStep);
        releaseRequestRepository.save(releaseRequest);
    }

    /**
     * Updates the status of a release request
     */
    public void updateStatus(Long releaseRequestId, ReleaseRequestStatus currentStatus) {
        ReleaseRequestDB releaseRequest = getReleaseRequestById(releaseRequestId);
        releaseRequest.setStatus(currentStatus);
        releaseRequest.setStatusDescription(ReleaseRequestStatusCode.IN_PROGRESS);
        releaseRequestRepository.save(releaseRequest);
    }

    /**
     * Marks a release request as failed with the provided reason code
     */
    public void failReleaseRequest(Long releaseRequestId, ReleaseRequestStatusCode reasonCode) {
        ReleaseRequestDB releaseRequest = getReleaseRequestById(releaseRequestId);
        releaseRequest.setStatus(ReleaseRequestStatus.FAILED);
        releaseRequest.setStatusDescription(reasonCode);
        releaseRequestRepository.save(releaseRequest);
    }

    /**
     * Marks a release request as completed
     */
    public void terminate(Long releaseRequestId) {
        ReleaseRequestDB releaseRequest = getReleaseRequestById(releaseRequestId);
        releaseRequest.setStatus(ReleaseRequestStatus.COMPLETED);
        releaseRequest.setStatusDescription(ReleaseRequestStatusCode.COMPLETED);
        releaseRequest.setCurrentStep(ReleaseRequestStep.FINISHED);
        releaseRequestRepository.save(releaseRequest);
    }
    /**
     * Deletes a failed release request by its ID
     */
    public void deleteFailedReleaseRequestsById(Long releaseRequestId) {
        ReleaseRequestDB releaseRequest = getReleaseRequestById(releaseRequestId);
        if (!ReleaseRequestStatus.FAILED.equals(releaseRequest.getStatus())) {
            throw new ReleaseRequestStatusException("Can't delete ReleaseRequest when status is " + releaseRequest.getStatus());
        }
        releaseRequestRepository.deleteById(releaseRequestId);
    }

    @Transactional(readOnly = true)
    public ReleaseRequestDB getReleaseRequestWithModesById(Long id) {
        return releaseRequestRepository.findWithModesById(id)
                .orElseThrow(() -> new ReleaseRequestNotFoundException(id));
    }

}
