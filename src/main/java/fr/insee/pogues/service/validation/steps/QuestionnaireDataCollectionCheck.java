package fr.insee.pogues.service.validation.steps;

import fr.insee.pogues.model.DataCollection;
import fr.insee.pogues.model.Questionnaire;
import fr.insee.pogues.model.Serie;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.service.metadata.MetadataService;
import fr.insee.pogues.service.validation.ValidationResult;
import fr.insee.pogues.service.validation.ValidationStep;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class QuestionnaireDataCollectionCheck implements ValidationStep {

    private MetadataService metadataService;

    @Override
    public ValidationResult validate(Questionnaire questionnaire) {
        return validateDataCollection(questionnaire.getDataCollection());
    }

    public ValidationResult validateDataCollection(DataCollection dataCollection){
        String serieId = Optional.ofNullable(dataCollection)
                .map(DataCollection::getSerie)
                .map(Serie::getId)
                .orElse(null);

        List<String> existingSerieIds = metadataService.getAllSeries().stream().map(SerieDto::id).toList();

        if(serieId != null && !existingSerieIds.contains(serieId)){
            return ValidationResult.invalid(String.format("Serie of id:\"%s\" doesn't exist in metadata repository", serieId));
        }
        return ValidationResult.valid();
    }


}
