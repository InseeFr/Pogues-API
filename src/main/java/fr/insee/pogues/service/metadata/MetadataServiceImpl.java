package fr.insee.pogues.service.metadata;

import fr.insee.pogues.client.metadata.DDIASClient;
import fr.insee.pogues.client.metadata.MagmaFusionClient;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.configuration.cache.CacheName;
import fr.insee.pogues.domain.entity.db.DDIAgencyDB;
import fr.insee.pogues.domain.entity.db.InternalSerieDB;
import fr.insee.pogues.exception.metadata.*;
import fr.insee.pogues.mapper.SerieMapper;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.persistence.repository.jpa.DDIAgencyRepository;
import fr.insee.pogues.persistence.repository.jpa.InternalSerieRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
    private InternalSerieRepository internalSerieRepository;
    private SerieMapper serieMapper;

    private static final String INTERNAL_SERIE_PREFIX_ID = "pogues_";
    private static final Pattern INTERNAL_SERIE_ID_PATTERN =
            Pattern.compile("^" + INTERNAL_SERIE_PREFIX_ID + "[a-zA-Z0-9]+$");

    @Override
    @Cacheable(CacheName.UNITS)
    public List<Unit> getUnits() throws Exception {
        return ddiasClient.getUnits();
    }

    @Override
    @Cacheable(CacheName.SERIES)
    public List<SerieDto> getAllSeries() {
        List<Serie> seriesFromRmes = magmaFusionClient.getSeries();
        List<InternalSerieDB> internalSeries = internalSerieRepository.findAll();

        List<SerieDto> seriesResult = new ArrayList<>();

        for (Serie serie : seriesFromRmes) {
            seriesResult.add(serieMapper.toDto(serie));
        }

        for(InternalSerieDB serie: internalSeries){
            seriesResult.add(serieMapper.toDto(serie));
        }
        return seriesResult;
    }

    @Override
    @Cacheable(CacheName.SERIE)
    public SerieDto getSerieDetailsById(String serieId) {

        if(serieId != null && serieId.startsWith(INTERNAL_SERIE_PREFIX_ID)){
            InternalSerieDB internalSerie = internalSerieRepository
                    .findById(serieId)
                    .orElseThrow(() -> new SerieNotFoundException(String.format("Serie (id:%s) not found", serieId)));

            return serieMapper.toDto(internalSerie);
        }
        return serieMapper.toDto(magmaFusionClient.getSerieById(serieId));
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
    public Boolean existAgencyMatchingById(String id) {
        return ddiAgencyRepository.existsMatchingDomain(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheName.SERIES, allEntries = true)
    public SerieDto createInternalSerie(SerieDto serie) {
        validateInternalSerieId(serie.id());
        validateAltLabelShorterThanLabel(serie);

        if (internalSerieRepository.existsById(serie.id())) {
            throw new InternalSerieAlreadyExists("Internal serie with id: " + serie.id() + " already exists");
        }

        InternalSerieDB serieDB = new InternalSerieDB(serie.id(), serie.uri(), serie.label(), serie.altLabel());
        InternalSerieDB created = internalSerieRepository.save(serieDB);
        return serieMapper.toDto(created);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheName.SERIE, key = "#id"),
            @CacheEvict(value = CacheName.SERIES, allEntries = true)
    })
    public Boolean deleteInternalSerieById(String id) {
        if(!internalSerieRepository.existsById(id)){
            throw new InternalSerieNotFound("Internal serie with id: "+id+" doesn't exist");
        }
        internalSerieRepository.deleteById(id);
        return true;
    }

    private void validateInternalSerieId(String serieId) {
        if(serieId == null || !INTERNAL_SERIE_ID_PATTERN.matcher(serieId).matches()){
            throw new InternalSerieInvalid(
                    "Internal serie id must start with '"+INTERNAL_SERIE_PREFIX_ID
                            +"' and contain only letters or digits, id: "+serieId);
        }
    }

    private void validateAltLabelShorterThanLabel(SerieDto serie) {
        String label = serie.label();
        String altLabel = serie.altLabel();
        if(altLabel == null || altLabel.isEmpty()){
            throw new InternalSerieInvalid(
                    "Internal serie altLabel must be defined, id: "+serie.id());
        }
        if(label != null && altLabel.length() > label.length()){
            throw new InternalSerieInvalid(
                    "Internal serie altLabel must be shorter than label, id: "+serie.id());
        }
    }
}
