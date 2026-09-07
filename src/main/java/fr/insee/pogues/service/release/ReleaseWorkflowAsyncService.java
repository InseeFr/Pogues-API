package fr.insee.pogues.service.release;

import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.exception.PoguesDeserializationException;
import fr.insee.pogues.exception.PoguesSerializationException;
import fr.insee.pogues.exception.questionnaire.composition.DeReferencingException;
import fr.insee.pogues.exception.questionnaire.composition.NullReferenceException;
import fr.insee.pogues.exception.release.steps.*;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import fr.insee.pogues.service.release.context.ReleaseModeWorkflowContext;
import fr.insee.pogues.service.release.context.ReleaseWorkflowContext;
import fr.insee.pogues.service.release.steps.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class ReleaseWorkflowAsyncService {

    private final ReleasePublicationService releasePublicationService;
    private final ReleaseModeService releaseModeService;
    private final IQuestionnaireService questionnaireService;
    private final VersionService versionService;
    private final ReleaseWorkflowStepRegistry workflowStepRegistry;
    private final ReleaseModeWorkflowStepRegistry modeWorkflowStepRegistry;

    private static final Map<Class<? extends Exception>, ReleaseRequestStatusCode> FAILURE_STATUS_CODES = Map.of(
            GenerateDDIException.class, ReleaseRequestStatusCode.ERROR_GENERATION_DDI,
            GenerateLunaticException.class, ReleaseRequestStatusCode.ERROR_GENERATION_LUNATIC,
            GenerateParametersException.class, ReleaseRequestStatusCode.ERROR_GENERATION_PARAMETERS,
            PublishDDIException.class, ReleaseRequestStatusCode.ERROR_PUBLICATION_DDI,
            PublishLunaticException.class, ReleaseRequestStatusCode.ERROR_PUBLICATION_LUNATIC,
            PublishPreReleaseException.class, ReleaseRequestStatusCode.ERROR_PUBLICATION_PRERELEASE
    );

    @Async("releaseProcessExecutor")
    public void publishRelease(Long releaseRequestId) {
        launchPublishWorkflow(releaseRequestId);
    }


    private void launchPublishWorkflow(Long releaseRequestId) {
        log.info("Publish Release workflow id: {}", releaseRequestId);
        try {
            ReleaseRequestDB request = releasePublicationService.getReleaseRequestWithModesById(releaseRequestId);
            ReleaseWorkflowContext context = initContext(request);

            releasePublicationService.updateStatus(releaseRequestId, ReleaseRequestStatus.RUNNING);

            executeCommonSteps(context);
            executeModeSteps(context, request.getModes());

            log.info("Release workflow id: {} - Completed", releaseRequestId);
            releasePublicationService.terminate(releaseRequestId);
        } catch (Exception e) {
            handleWorkflowFailure(releaseRequestId, e);
        }
    }

    private void executeCommonSteps(ReleaseWorkflowContext context) {
        Long releaseRequestId = context.getReleaseRequestId();
        for (ReleaseWorkflowStep step : workflowStepRegistry.orderedSteps()) {
            log.info("Release workflow id: {} - step: {} started", releaseRequestId, step.getStep());
            long start = System.currentTimeMillis();

            releasePublicationService.updateStep(releaseRequestId, step.getStep());
            step.execute(context);
            step.afterExecute(context);

            log.info("Release workflow id: {} - step: {} completed in {} ms",
                    releaseRequestId, step.getStep(), System.currentTimeMillis() - start);
        }
    }

    private void executeModeSteps(ReleaseWorkflowContext context, List<ReleaseRequestModeDB> modes) {
        releasePublicationService.updateStep(context.getReleaseRequestId(), ReleaseRequestStep.PUBLISH_MODES);
        for (ReleaseRequestModeDB mode : modes) {
            executeStepsForMode(context, mode);
        }
    }

    private void executeStepsForMode(ReleaseWorkflowContext context, ReleaseRequestModeDB mode) {
        Long releaseRequestId = context.getReleaseRequestId();
        ReleaseModeWorkflowContext modeContext = new ReleaseModeWorkflowContext(context, mode.getId(), mode.getMode());

        releaseModeService.updateStatus(mode.getId(), ReleaseRequestStatus.RUNNING);

        for (ReleaseModeWorkflowStep step : modeWorkflowStepRegistry.orderedSteps()) {
            log.info("Release workflow id: {} - mode: {} - step: {} started",
                    releaseRequestId, mode.getMode(), step.getStep());
            long start = System.currentTimeMillis();

            releaseModeService.updateStep(mode.getId(), step.getStep());
            step.execute(modeContext);
            step.afterExecute(modeContext);

            if (ReleaseRequestModeStep.PUBLICATION_PRERELEASE.equals(step.getStep())) {
                releaseModeService.updateCollectionInstrumentId(mode.getId(), modeContext.getCollectionInstrumentId());
            }

            log.info("Release workflow id: {} - mode: {} - step: {} completed in {} ms",
                    releaseRequestId, mode.getMode(), step.getStep(), System.currentTimeMillis() - start);
        }
        releaseModeService.terminate(mode.getId());
    }



    void handleWorkflowFailure(Long releaseRequestId, Exception e) {
        log.error("Release workflow id: {} - Failed", releaseRequestId, e);
        ReleaseRequestStatusCode statusCode = FAILURE_STATUS_CODES.getOrDefault(e.getClass(), ReleaseRequestStatusCode.DEFAULT_FAILURE_STATUS_CODE);
        releasePublicationService.failReleaseRequest(releaseRequestId, statusCode);
    }

    private ReleaseWorkflowContext initContext(ReleaseRequestDB request) throws PoguesDeserializationException, PoguesSerializationException, DeReferencingException, NullReferenceException {
        Questionnaire questionnaireVersionModel = versionService.getVersionDataQuestionnaireModelByVersionId(request.getPoguesVersionId());
        Questionnaire poguesQuestionnaire = questionnaireService.getQuestionnaireWithItsReferences(questionnaireVersionModel);

        GenerationParameters commonParameters = GenerationParameters.builder()
                .context(request.getContext())
                .questionNumberingMode(request.getGenerationParamsQuestionNumberingMode())
                .responseTimeQuestion(request.getGenerationParamsResponseTimeQuestion())
                .build();

        log.info("Release workflow id: {} - WorkflowContext initialized", request.getId());

        return new ReleaseWorkflowContext(
                request.getId(),
                request.getPoguesVersionId(),
                request.getReleaseDescription(),
                poguesQuestionnaire,
                commonParameters);
    }
}
