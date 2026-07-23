package fr.insee.pogues.service;


import fr.insee.pogues.exception.validation.ModelValidationException;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.persistence.service.IQuestionnaireService;
import fr.insee.pogues.persistence.service.VersionService;
import fr.insee.pogues.service.validation.ValidationResult;
import fr.insee.pogues.service.validation.steps.QuestionnaireDDIAgencyCheck;
import fr.insee.pogues.service.validation.steps.QuestionnaireDataCollectionCheck;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                questionnaire.getFlowLogic(),
                questionnaire.getFormulasLanguage(),
                questionnaire.getDataCollection(),
                questionnaire.getTargetMode(),
                questionnaire.getAgency(),
                questionnaire.getOwner()
        );
    }

    public void updateQuestionnaireDetailsById(String poguesId, QuestionnaireDetailsDto questionnaireDetailsDto) throws Exception {
        validateDetails(questionnaireDetailsDto);

        Questionnaire questionnaire = questionnaireService.getQuestionnaireModelByID(poguesId);
        questionnaire.setName(questionnaireDetailsDto.name());

        questionnaire.getLabel().clear();
        questionnaire.getLabel().add(questionnaireDetailsDto.label());

        questionnaire.setFlowLogic(questionnaireDetailsDto.flowLogic());
        questionnaire.setFormulasLanguage(questionnaireDetailsDto.formulasLanguage());
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
                questionnaire.getFlowLogic(),
                questionnaire.getFormulasLanguage(),
                questionnaire.getDataCollection(),
                questionnaire.getTargetMode(),
                questionnaire.getAgency(),
                questionnaire.getOwner()
        );
    }

    private void validateDetails(QuestionnaireDetailsDto questionnaireDetailsDto){

        List<String> errorMessages = new ArrayList<>();

        ValidationResult dataCollectionCheck =  questionnaireDataCollectionCheck.validateDataCollection(questionnaireDetailsDto.dataCollection());
        if(!dataCollectionCheck.isValid()) errorMessages.add(dataCollectionCheck.errorMessage());

        ValidationResult ddiAgencyCheck =  questionnaireDDIAgencyCheck.validateAgency(questionnaireDetailsDto.agency());

        if(!ddiAgencyCheck.isValid()) errorMessages.add(ddiAgencyCheck.errorMessage());

        if(!errorMessages.isEmpty()){
            throw new ModelValidationException("Questionnaire details invalid", errorMessages);
        }
    }

}
