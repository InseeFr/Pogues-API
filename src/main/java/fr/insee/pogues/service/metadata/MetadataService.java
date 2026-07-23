package fr.insee.pogues.service.metadata;

import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;

import java.util.List;

public interface MetadataService {

    List<Unit> getUnits() throws Exception;
    List<SerieDto> getAllSeries();
    SerieDto getSerieDetailsById(String serieId);
    List<AgencyDto> getAgencies(String countryFilter);
    AgencyDto createAgency(AgencyDto agency);
    Boolean deleteAgencyById(String id);
    Boolean existAgencyById(String id);
}
