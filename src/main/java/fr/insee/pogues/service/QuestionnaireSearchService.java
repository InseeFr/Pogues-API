package fr.insee.pogues.service;

import fr.insee.pogues.mapper.QuestionnaireSearchResultMapper;
import fr.insee.pogues.model.dto.questionnaires.QuestionnaireSearchResultDTO;
import fr.insee.pogues.persistence.repository.jpa.QuestionnaireJpaRepository;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class QuestionnaireSearchService {

    private final QuestionnaireJpaRepository questionnaireJpaRepository;
    private final QuestionnaireSearchResultMapper questionnaireSearchResultMapper;

    /**
     * Search questionnaires matching the given filters. A null filter is ignored.
     * @param owner stamp owning the questionnaires
     * @param serieId id of the serie of the questionnaires
     * @return the light representation of the matching questionnaires
     */
    public List<QuestionnaireSearchResultDTO> searchQuestionnaires(@Nullable String owner, @Nullable String serieId) {
        return questionnaireJpaRepository.searchQuestionnaires(owner, serieId).stream()
                .map(questionnaireSearchResultMapper::toDto)
                .toList();
    }
}
