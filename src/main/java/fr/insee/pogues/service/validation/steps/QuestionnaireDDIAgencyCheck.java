package fr.insee.pogues.service.validation.steps;

import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.service.metadata.MetadataService;
import fr.insee.pogues.service.validation.ValidationResult;
import fr.insee.pogues.service.validation.ValidationStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class QuestionnaireDDIAgencyCheck implements ValidationStep {

    private MetadataService metadataService;

    @Override
    public ValidationResult validate(Questionnaire questionnaire) {
        return validateAgency(questionnaire.getAgency());
    }

    public ValidationResult validateAgency(String agency){
        if(agency != null && Boolean.FALSE.equals(metadataService.existAgencyById(agency))){
            return ValidationResult.invalid(String.format("DDI Agency \"%s\" doesn't exist", agency));
        }
        return ValidationResult.valid();
    }
}
