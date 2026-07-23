package fr.insee.pogues.service.metadata;

import fr.insee.pogues.client.metadata.DDIASClient;
import fr.insee.pogues.client.metadata.MagmaFusionClient;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.client.metadata.model.magma.fusion.Label;
import fr.insee.pogues.client.metadata.model.magma.fusion.Operation;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import fr.insee.pogues.domain.entity.db.DDIAgencyDB;
import fr.insee.pogues.exception.metadata.DDIAgencyAlreadyExists;
import fr.insee.pogues.exception.metadata.DDIAgencyNotFound;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.OperationDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.persistence.repository.DDIAgencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    private MetadataServiceImpl metadataService;

    @BeforeEach
    void init() {
        metadataService = new MetadataServiceImpl(ddiasClient, magmaFusionClient, ddiAgencyRepository);
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

        Serie serie1 = new Serie(
                "S1",
                "uri:s1",
                List.of(
                        new Label("Population", "en"),
                        new Label("Population française", "fr")
                )
        );

        Serie serie2 = new Serie(
                "S2",
                "uri:s2",
                List.of(
                        new Label("Logements", "fr")
                )
        );

        when(magmaFusionClient.getSeries())
                .thenReturn(List.of(serie1, serie2));

        List<SerieDto> result = metadataService.getAllSeries();

        assertEquals(2, result.size());

        assertEquals("S1", result.getFirst().id());
        assertEquals("uri:s1", result.getFirst().uri());
        assertEquals("Population française", result.getFirst().label());
        assertNull(result.get(0).altLabel());
        assertNull(result.get(0).operations());

        assertEquals("S2", result.get(1).id());
        assertEquals("Logements", result.get(1).label());

        verify(magmaFusionClient).getSeries();
    }

    @Test
    @DisplayName("Should return complete serie details")
    void getSerieDetailsById_success() {

        Operation operation = new Operation(
                "O1",
                "uri:o1",
                List.of(
                        new Label("Recensement", "fr")
                )
        );

        SerieMetadata metadata = new SerieMetadata(
                "S1",
                "uri:s1",
                List.of(new Label("Population","fr")),
                List.of(new Label("POP", "fr")),
                List.of(operation)
        );

        when(magmaFusionClient.getSerieById("S1"))
                .thenReturn(metadata);

        SerieDto result = metadataService.getSerieDetailsById("S1");

        assertEquals("S1", result.id());
        assertEquals("uri:s1", result.uri());
        assertEquals("Population", result.label());
        assertEquals("POP", result.altLabel());

        assertEquals(1, result.operations().size());

        OperationDto operationDto = result.operations().getFirst();

        assertEquals("O1", operationDto.id());
        assertEquals("uri:o1", operationDto.uri());
        assertEquals("Recensement", operationDto.label());

        verify(magmaFusionClient).getSerieById("S1");
    }

    @Test
    @DisplayName("Should return empty label when french label does not exist")
    void getSerieDetailsById_withoutFrenchLabel() {

        SerieMetadata metadata = new SerieMetadata(
                "S1",
                "uri:s1",
                List.of(new Label("en", "Population")),
                List.of(),
                List.of()
        );

        when(magmaFusionClient.getSerieById("S1"))
                .thenReturn(metadata);

        SerieDto result = metadataService.getSerieDetailsById("S1");

        assertEquals("", result.label());
        assertEquals("", result.altLabel());
        assertTrue(result.operations().isEmpty());
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
}
