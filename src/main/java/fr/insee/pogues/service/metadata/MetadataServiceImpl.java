package fr.insee.pogues.service.metadata;

import fr.insee.pogues.client.metadata.DDIASClient;
import fr.insee.pogues.client.metadata.MagmaFusionClient;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.client.metadata.model.magma.fusion.Label;
import fr.insee.pogues.client.metadata.model.magma.fusion.Operation;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import fr.insee.pogues.configuration.cache.CacheName;
import fr.insee.pogues.domain.entity.db.DDIAgencyDB;
import fr.insee.pogues.exception.metadata.DDIAgencyAlreadyExists;
import fr.insee.pogues.exception.metadata.DDIAgencyNotFound;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.OperationDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.persistence.repository.DDIAgencyRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class MetadataServiceImpl implements MetadataService {

    /**
     * DDIAS (DDI Access Service) Client is used of units & codeLists (suggester)
     */
    private DDIASClient ddiasClient;
    private MagmaFusionClient magmaFusionClient;
    private DDIAgencyRepository ddiAgencyRepository;

    private static final String FR_LANGUAGE = "fr";


    private String findFrLabel(List<Label> labels) {
        Optional<Label> labelOptional = labels.stream()
                .filter(label -> FR_LANGUAGE.equals(label.langue()))
                .findFirst();

        if (labelOptional.isPresent()) return labelOptional.get().contenu();
        return "";
    }

    private OperationDto toOperationDto(Operation operation) {
        return new OperationDto(
                operation.id(),
                operation.uri(),
                findFrLabel(operation.label())
        );
    }


    @Override
    @Cacheable(CacheName.UNITS)
    public List<Unit> getUnits() throws Exception {
        return ddiasClient.getUnits();
    }

    @Override
    @Cacheable(CacheName.SERIES)
    public List<SerieDto> getAllSeries() {
        List<Serie> seriesFromRmes = magmaFusionClient.getSeries();

        List<SerieDto> seriesResult = new ArrayList<>();

        for (Serie serie : seriesFromRmes) {
            SerieDto serieDto = new SerieDto(
                    serie.seriesId(),
                    serie.uri(),
                    findFrLabel(serie.label()),
                    null,
                    null
            );
            seriesResult.add(serieDto);
        }
        return seriesResult;
    }

    @Override
    @Cacheable(CacheName.SERIE)
    public SerieDto getSerieDetailsById(String serieId) {
        SerieMetadata serieFromRmes = magmaFusionClient.getSerieById(serieId);

        return new SerieDto(
                serieFromRmes.seriesId(),
                serieFromRmes.uri(),
                findFrLabel(serieFromRmes.label()),
                findFrLabel(serieFromRmes.altLabel()),
                serieFromRmes.operations().stream().map(this::toOperationDto).toList());
    }

    @Override
    public List<AgencyDto> getAgencies(String countryFilter) {
        // see https://registry.ddialliance.org/Agency

        List<DDIAgencyDB> ddiAgencies = countryFilter == null
                ? ddiAgencyRepository.findAll()
                : ddiAgencyRepository.findByNameStartingWith(countryFilter);

        return ddiAgencies.stream()
                .map(ddiAgencyDB -> new AgencyDto(ddiAgencyDB.getName(), ddiAgencyDB.getLabel()))
                .toList();
    }

    @Override
    public AgencyDto createAgency(AgencyDto agency) {
        String agencyName = agency.id();
        if(ddiAgencyRepository.existsByName(agencyName)){
            throw new DDIAgencyAlreadyExists("DDI agency with id: "+agencyName+" already exists");
        }
        DDIAgencyDB agencyDB = new DDIAgencyDB();
        agencyDB.setName(agencyName);
        agencyDB.setLabel(agency.label());
        DDIAgencyDB ddiAgencyCreated = ddiAgencyRepository.save(agencyDB);
        return new AgencyDto(ddiAgencyCreated.getName(), ddiAgencyCreated.getLabel());
    }

    @Override
    @Transactional
    public Boolean deleteAgencyById(String id) {
        if(!ddiAgencyRepository.existsByName(id)){
            throw new DDIAgencyNotFound("DDI agency with id: "+id+" doesn't exist");
        }
        return ddiAgencyRepository.deleteByName(id) > 0;
    }

    @Override
    public Boolean existAgencyById(String id) {
        return ddiAgencyRepository.existsByName(id);
    }
}
