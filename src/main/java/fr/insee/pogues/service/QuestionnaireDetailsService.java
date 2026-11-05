package fr.insee.pogues.service;


import fr.insee.pogues.exception.details.QuestionnaireDetailsInvalidException;
import fr.insee.pogues.model.*;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import fr.insee.pogues.service.validation.ValidationResult;
import fr.insee.pogues.service.validation.steps.QuestionnaireDDIAgencyCheck;
import fr.insee.pogues.service.validation.steps.QuestionnaireDataCollectionCheck;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static fr.insee.pogues.utils.model.QuestionnaireInit.initQuestionnaire;

@Service
@Slf4j
@AllArgsConstructor
public class QuestionnaireDetailsService {

    private final IQuestionnaireService questionnaireService;
    private final VersionService versionService;
    private final QuestionnaireDDIAgencyCheck questionnaireDDIAgencyCheck;
    private final QuestionnaireDataCollectionCheck questionnaireDataCollectionCheck;

    public QuestionnaireDetailsDto getQuestionnaireDetailsById(String poguesId) throws Exception {
        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(poguesId);
        return new QuestionnaireDetailsDto(
                questionnaire.getId(),
                questionnaire.getName(),
                questionnaire.getLabel().getFirst(),
                questionnaire.getDataCollection(),
                questionnaire.getTargetMode(),
                questionnaire.getAgency(),
                questionnaire.getOwner()
        );
    }

    /**
     * Create a new (empty) questionnaire from its details.
     * @param questionnaireDetailsDto details of the questionnaire to create
     * @return details of the created questionnaire
     */
    public QuestionnaireDetailsDto createQuestionnaireFromDetails(QuestionnaireDetailsDto questionnaireDetailsDto) throws Exception {
        validateDetails(questionnaireDetailsDto);

        String poguesId = questionnaireDetailsDto.id();
        Questionnaire questionnaire = initQuestionnaire(questionnaireDetailsDto.targetMode());
        questionnaire.setId(poguesId);
        questionnaire.setName(questionnaireDetailsDto.name());
        questionnaire.getLabel().add(questionnaireDetailsDto.label());
        questionnaire.setOwner(questionnaireDetailsDto.owner());
        questionnaire.setAgency(questionnaireDetailsDto.agency());
        questionnaire.setDataCollection(questionnaireDetailsDto.dataCollection());

        questionnaireService.createQuestionnaire(questionnaire);
        return getQuestionnaireDetailsById(poguesId);
    }

    public void updateQuestionnaireDetailsById(String poguesId, QuestionnaireDetailsDto questionnaireDetailsDto) throws Exception {
        validateDetails(questionnaireDetailsDto);

        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(poguesId);
        questionnaire.setName(questionnaireDetailsDto.name());

        questionnaire.getLabel().clear();
        questionnaire.getLabel().add(questionnaireDetailsDto.label());

        questionnaire.setDataCollection(questionnaireDetailsDto.dataCollection());

        questionnaire.getTargetMode().clear();
        questionnaire.getTargetMode().addAll(questionnaireDetailsDto.targetMode());

        questionnaire.setAgency(questionnaireDetailsDto.agency());
        questionnaire.setOwner(questionnaireDetailsDto.owner());
        questionnaireService.updateQuestionnaire(poguesId, questionnaire);
    }

    public QuestionnaireDetailsDto getQuestionnaireDetailsByVersionId(UUID versionId) throws Exception {
        Questionnaire questionnaire = versionService.getVersionDataQuestionnaireModelByVersionId(versionId);
        return new QuestionnaireDetailsDto(
                questionnaire.getId(),
                questionnaire.getName(),
                questionnaire.getLabel().getFirst(),
                questionnaire.getDataCollection(),
                questionnaire.getTargetMode(),
                questionnaire.getAgency(),
                questionnaire.getOwner()
        );
    }

    /**
     * Check the details against the metadata repository (serie and DDI agency).
     * Format rules are checked on the DTO by Bean Validation.
     * @param questionnaireDetailsDto Details to check
     * @throws QuestionnaireDetailsInvalidException The details are not valid
     */
    private void validateDetails(QuestionnaireDetailsDto questionnaireDetailsDto) {
        validateDataCollection(questionnaireDetailsDto);
        validateAgency(questionnaireDetailsDto);
    }

    /** Check that the serie of the questionnaire, if provided, exists in the metadata repository. */
    private void validateDataCollection(QuestionnaireDetailsDto questionnaireDetailsDto) {
        ValidationResult result = questionnaireDataCollectionCheck.validateDataCollection(questionnaireDetailsDto.dataCollection());
        if (!result.isValid()) {
            throw new QuestionnaireDetailsInvalidException(result.errorMessage());
        }
    }

    /** Check that the DDI agency of the questionnaire exists in the metadata repository. */
    private void validateAgency(QuestionnaireDetailsDto questionnaireDetailsDto) {
        ValidationResult result = questionnaireDDIAgencyCheck.validateAgency(questionnaireDetailsDto.agency());
        if (!result.isValid()) {
            throw new QuestionnaireDetailsInvalidException(result.errorMessage());
        }
    }

}
