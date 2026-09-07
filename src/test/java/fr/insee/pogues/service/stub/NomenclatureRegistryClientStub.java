package fr.insee.pogues.service.stub;

import fr.insee.pogues.client.surveyregistry.nomenclature.NomenclatureRegistryClient;
import fr.insee.pogues.model.dto.nomenclatures.NomenclatureDTO;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
public class NomenclatureRegistryClientStub implements NomenclatureRegistryClient {
    List<NomenclatureDTO> nomenclatures = List.of();

    @Setter
    NomenclatureDTO nomenclatureToReturn = null;

    @Override
    public List<NomenclatureDTO> getNomenclatures() {
        return nomenclatures;
    }

    @Override
    public NomenclatureDTO getNomenclatureMetadataById(UUID id) {
        return nomenclatureToReturn;
    }

}
