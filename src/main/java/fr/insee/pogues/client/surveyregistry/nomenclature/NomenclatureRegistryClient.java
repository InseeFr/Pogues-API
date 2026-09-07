package fr.insee.pogues.client.surveyregistry.nomenclature;

import fr.insee.pogues.model.dto.nomenclatures.NomenclatureDTO;

import java.util.List;
import java.util.UUID;

/** Client to access nomenclature registry data. */
public interface NomenclatureRegistryClient {
    List<NomenclatureDTO> getNomenclatures();
    NomenclatureDTO getNomenclatureMetadataById(UUID id);
}
