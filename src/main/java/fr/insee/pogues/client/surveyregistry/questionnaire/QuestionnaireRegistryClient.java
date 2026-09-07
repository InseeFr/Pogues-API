package fr.insee.pogues.client.surveyregistry.questionnaire;

import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentCreateDto;
import fr.insee.pogues.client.surveyregistry.model.CollectionInstrumentMetadataDto;
import fr.insee.pogues.client.surveyregistry.model.ConceptualModelDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client to access registry data. */
public interface QuestionnaireRegistryClient {
    Optional<ConceptualModelDto> findConceptualModelByPoguesVersionId(UUID poguesVersionId);

    void createConceptualModel(ConceptualModelDto conceptualModel);
    void publishDDIInConceptualModel(UUID poguesVersionId, String ddi);

    UUID createCollectionInstrumentMetadata(CollectionInstrumentCreateDto collectionInstrumentCreate);
    void publishLunaticInCollectionInstrument(UUID collectionInstrumentId, String lunatic);

    List<CollectionInstrumentMetadataDto> getCollectionInstrumentByPoguesId(String poguesId, boolean withCodesLists);
}
