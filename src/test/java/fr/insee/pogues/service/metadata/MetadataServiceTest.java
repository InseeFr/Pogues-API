package fr.insee.pogues.service.metadata;

import fr.insee.pogues.client.metadata.DDIASClient;
import fr.insee.pogues.client.metadata.MagmaFusionClient;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.client.metadata.model.magma.fusion.Label;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import fr.insee.pogues.domain.entity.db.DDIAgencyDB;
import fr.insee.pogues.domain.entity.db.InternalSerieDB;
import fr.insee.pogues.exception.metadata.*;
import fr.insee.pogues.mapper.SerieMapper;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.persistence.repository.jpa.DDIAgencyRepository;
import fr.insee.pogues.persistence.repository.jpa.InternalSerieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetadataServiceTest {

    @Mock
    private DDIASClient ddiasClient;

    @Mock
    private MagmaFusionClient magmaFusionClient;

    @Mock
    private DDIAgencyRepository ddiAgencyRepository;

    @Mock
    private InternalSerieRepository internalSerieRepository;

    private MetadataServiceImpl metadataService;

    @BeforeEach
    void init() {
        metadataService = new MetadataServiceImpl(ddiasClient, magmaFusionClient, ddiAgencyRepository, internalSerieRepository, new SerieMapper());
    }

    @Test
    @DisplayName("Should return units from DDIAS")
    void getUnits_success() throws Exception {

        List<Unit> units = List.of(
                new Unit("uri:1", "kg"),
                new Unit("uri:2", "€")
        );

        when(ddiasClient.getUnits()).thenReturn(units);

        List<Unit> result = metadataService.getUnits();

        assertEquals(units, result);

        verify(ddiasClient).getUnits();
        verifyNoInteractions(magmaFusionClient);
    }

    @Test
    @DisplayName("Should convert all series into DTO")
    void getAllSeries_success() {
        when(magmaFusionClient.getSeries())
                .thenReturn(List.of(
                        new Serie(
                                "S1",
                                "uri:s1",
                                List.of(
                                        new Label("Population", "en"),
                                        new Label("Population française", "fr")
                                )),
                        new Serie(
                                "S2",
                                "uri:s2",
                                List.of(
                                        new Label("Logements", "fr")
                                )
                        )));
        when(internalSerieRepository.findAll())
                .thenReturn(List.of(new InternalSerieDB("pogues_s1", "uri", "label pogues", "ALT_LABEL")));

        List<SerieDto> result = metadataService.getAllSeries();

        assertThat(result).hasSize(3);
        assertEquals("S1", result.getFirst().id());
        assertEquals("uri:s1", result.getFirst().uri());
        assertEquals("Population française", result.getFirst().label());
        assertNull(result.get(0).altLabel());

        assertEquals("S2", result.get(1).id());
        assertEquals("Logements", result.get(1).label());

        assertEquals("pogues_s1", result.get(2).id());
        assertEquals("label pogues", result.get(2).label());

        verify(magmaFusionClient).getSeries();
    }

    @Test
    @DisplayName("Should return complete serie details")
    void getSerieDetailsById_success() {

        SerieMetadata metadata = new SerieMetadata(
                "S1",
                "uri:s1",
                List.of(new Label("Population","fr")),
                List.of(new Label("POP", "fr"))
        );

        when(magmaFusionClient.getSerieById("S1"))
                .thenReturn(metadata);

        SerieDto result = metadataService.getSerieDetailsById("S1");

        assertEquals("S1", result.id());
        assertEquals("uri:s1", result.uri());
        assertEquals("Population", result.label());
        assertEquals("POP", result.altLabel());

        verify(magmaFusionClient).getSerieById("S1");
    }

    @Test
    @DisplayName("Should return empty label when french label does not exist")
    void getSerieDetailsById_withoutFrenchLabel() {

        SerieMetadata metadata = new SerieMetadata(
                "S1",
                "uri:s1",
                List.of(new Label("en", "Population")),
                List.of()
        );

        when(magmaFusionClient.getSerieById("S1"))
                .thenReturn(metadata);

        SerieDto result = metadataService.getSerieDetailsById("S1");

        assertEquals("", result.label());
        assertEquals("", result.altLabel());
    }

    @Test
    @DisplayName("Should return All Agencies")
    void getAllAgencies_sucess() {

        when(ddiAgencyRepository.findAll())
                .thenReturn(List.of(
                        new DDIAgencyDB(1L, "fr.insee", "INSEE"),
                        new DDIAgencyDB(2L, "fr.inserm", "Institut National de la Recherche Médicale"),
                        new DDIAgencyDB(3L, "fr.cdsp", "Sciences Po, Center for Socio-Political Data (CDSP), CNRS"),

                        new DDIAgencyDB(4L, "int.ddibestpractices", "DDI Best Practices"),
                        new DDIAgencyDB(5L, "de.sinus-institut", "SINUS Markt- und Sozialforschung GmbH"),
                        new DDIAgencyDB(6L, "us.census", "U.S. Census Bureau")
                ));

        List<AgencyDto> result = metadataService.getAgencies(null);
        assertThat(result).hasSize(6);
        assertThat(result).contains(new AgencyDto("fr.insee", "INSEE"));
        assertThat(result).contains(new AgencyDto("int.ddibestpractices", "DDI Best Practices"));
    }

    @Test
    @DisplayName("Should return French Agencies")
    void getFrAgencies_sucess() {

        when(ddiAgencyRepository.findByNameStartingWith("fr"))
                .thenReturn(List.of(
                        new DDIAgencyDB(1L, "fr.insee", "INSEE"),
                        new DDIAgencyDB(2L, "fr.inserm", "Institut National de la Recherche Médicale"),
                        new DDIAgencyDB(3L, "fr.cdsp", "Sciences Po, Center for Socio-Political Data (CDSP), CNRS")
                ));

        List<AgencyDto> result = metadataService.getAgencies("fr");
        assertThat(result).hasSize(3);
        assertThat(result).contains(new AgencyDto("fr.insee", "INSEE"));
    }

    @Test
    @DisplayName("Should create Agency")
    void createAgency_success(){
        when(ddiAgencyRepository.existsByName("fr.insee"))
                .thenReturn(false);
        when(ddiAgencyRepository.save(any()))
                .thenReturn(new DDIAgencyDB(1L, "fr.insee", "INSEE"));

        AgencyDto result = metadataService.createAgency(new AgencyDto("fr.insee", "INSEE"));
        assertEquals(new AgencyDto("fr.insee", "INSEE"), result);
    }

    @Test
    @DisplayName("Should not create Agency with existing name")
    void createAgency_failed(){
        when(ddiAgencyRepository.existsByName("fr.insee"))
                .thenReturn(true);

        Executable executable = () ->  metadataService.createAgency(new AgencyDto("fr.insee", "INSEE"));

        assertThrows(DDIAgencyAlreadyExists.class, executable);
    }

    @Test
    @DisplayName("Should not delete Agency with not existing name")
    void deleteAgency_failed(){
        when(ddiAgencyRepository.existsByName("fr.insee"))
                .thenReturn(false);

        Executable executable = () ->  metadataService.deleteAgencyById("fr.insee");

        assertThrows(DDIAgencyNotFound.class, executable);
    }

    @Test
    @DisplayName("Should delete Agency with existing name")
    void deleteAgency_success(){
        when(ddiAgencyRepository.existsByName("fr.insee"))
                .thenReturn(true);
        when(ddiAgencyRepository.deleteByName("fr.insee"))
                .thenReturn(1);

        Boolean isDeleted = metadataService.deleteAgencyById("fr.insee");

        assertTrue(isDeleted);
    }

    @Test
    @DisplayName("Should return internal serie details when id starts with the internal serie prefix")
    void getSerieDetailsById_internalSerie_success() {
        String serieId = "pogues_s1";

        InternalSerieDB internalSerie = new InternalSerieDB(
                serieId,
                "uri:internal:s1",
                "Série interne",
                "SERIE_INTERNE"
        );

        when(internalSerieRepository.findById(serieId))
                .thenReturn(Optional.of(internalSerie));

        SerieDto result = metadataService.getSerieDetailsById(serieId);

        assertThat(result)
                .isEqualTo(new SerieDto(
                        serieId,
                        "uri:internal:s1",
                        "Série interne",
                        "SERIE_INTERNE"
                ));

        verify(internalSerieRepository).findById(serieId);
        verifyNoInteractions(magmaFusionClient);
    }

    @Test
    @DisplayName("Should throw SerieNotFoundException when internal serie does not exist")
    void getSerieDetailsById_internalSerie_notFound() {
        String serieId = "pogues_unknown";

        when(internalSerieRepository.findById(serieId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> metadataService.getSerieDetailsById(serieId))
                .isInstanceOf(SerieNotFoundException.class)
                .hasMessage("Serie (id:pogues_unknown) not found");

        verify(internalSerieRepository).findById(serieId);
        verifyNoInteractions(magmaFusionClient);
    }

    @Test
    @DisplayName("Should create internal serie when id, label and altLabel are valid")
    void createInternalSerie_success() {
        SerieDto input = new SerieDto("pogues_s1", "uri:s1", "Population française", "POP");

        when(internalSerieRepository.existsById("pogues_s1")).thenReturn(false);
        when(internalSerieRepository.save(any()))
                .thenReturn(new InternalSerieDB("pogues_s1", "uri:s1", "Population française", "POP"));

        SerieDto result = metadataService.createInternalSerie(input);

        assertThat(result.id()).isEqualTo("pogues_s1");
        assertThat(result.label()).isEqualTo("Population française");
        assertThat(result.altLabel()).isEqualTo("POP");
    }

    @Test
    @DisplayName("Should reject id not starting with the internal serie prefix")
    void createInternalSerie_invalidPrefix() {
        SerieDto input = new SerieDto("s1", "uri:s1", "Population", "POP");

        assertThatThrownBy(() -> metadataService.createInternalSerie(input))
                .isInstanceOf(InternalSerieInvalid.class);

        verifyNoInteractions(internalSerieRepository);
    }

    @Test
    @DisplayName("Should reject id containing spaces or special characters")
    void createInternalSerie_invalidCharacters() {
        SerieDto input = new SerieDto("pogues_s 1!", "uri:s1", "Population", "POP");

        assertThatThrownBy(() -> metadataService.createInternalSerie(input))
                .isInstanceOf(InternalSerieInvalid.class);

        verifyNoInteractions(internalSerieRepository);
    }

    @Test
    @DisplayName("Should reject altLabel longer than or equal to label")
    void createInternalSerie_altLabelTooLong() {
        SerieDto input = new SerieDto("pogues_s1", "uri:s1", "Pop", "Population");

        assertThatThrownBy(() -> metadataService.createInternalSerie(input))
                .isInstanceOf(InternalSerieInvalid.class);

        verifyNoInteractions(internalSerieRepository);
    }

    @Test
    @DisplayName("Should reject altLabel null or empty")
    void createInternalSerie_altLabelBlank() {
        SerieDto input = new SerieDto("pogues_s1", "uri:s1", "Population", "");

        assertThatThrownBy(() -> metadataService.createInternalSerie(input))
                .isInstanceOf(InternalSerieInvalid.class)
                .hasMessage("Internal serie altLabel must be defined, id: pogues_s1");

        verifyNoInteractions(internalSerieRepository);
    }

    @Test
    @DisplayName("Should reject altLabel null")
    void createInternalSerie_altLabelNull() {
        SerieDto input = new SerieDto("pogues_s1", "uri:s1", "Population", null);

        assertThatThrownBy(() -> metadataService.createInternalSerie(input))
                .isInstanceOf(InternalSerieInvalid.class)
                .hasMessage("Internal serie altLabel must be defined, id: pogues_s1");

        verifyNoInteractions(internalSerieRepository);
    }

    @Test
    @DisplayName("Should throw conflict when internal serie id already exists")
    void createInternalSerie_alreadyExists() {
        SerieDto input = new SerieDto("pogues_s1", "uri:s1", "Population", "POP");

        when(internalSerieRepository.existsById("pogues_s1")).thenReturn(true);

        assertThatThrownBy(() -> metadataService.createInternalSerie(input))
                .isInstanceOf(InternalSerieAlreadyExists.class);

        verify(internalSerieRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete internal serie when id exists")
    void deleteInternalSerie_success() {
        when(internalSerieRepository.existsById("pogues_s1")).thenReturn(true);

        Boolean result = metadataService.deleteInternalSerieById("pogues_s1");

        assertThat(result).isTrue();
        verify(internalSerieRepository).deleteById("pogues_s1");
    }

    @Test
    @DisplayName("Should throw not found when internal serie id does not exist")
    void deleteInternalSerie_notFound() {
        when(internalSerieRepository.existsById("pogues_unknown")).thenReturn(false);

        assertThatThrownBy(() -> metadataService.deleteInternalSerieById("pogues_unknown"))
                .isInstanceOf(InternalSerieNotFound.class);

        verify(internalSerieRepository, never()).deleteById(any());
    }
}
