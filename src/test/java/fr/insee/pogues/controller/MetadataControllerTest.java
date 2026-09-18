package fr.insee.pogues.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.insee.pogues.client.metadata.exceptions.MetadataRepositoryException;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.configuration.log.LogInterceptor;
import fr.insee.pogues.exception.metadata.DDIAgencyAlreadyExists;
import fr.insee.pogues.exception.metadata.DDIAgencyNotFound;
import fr.insee.pogues.exception.metadata.InternalSerieAlreadyExists;
import fr.insee.pogues.exception.metadata.InternalSerieInvalid;
import fr.insee.pogues.exception.metadata.InternalSerieNotFound;
import fr.insee.pogues.exception.metadata.MetadataControllerAdvice;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.service.metadata.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "testUser", roles = {"ADMIN"})
@WebMvcTest(MetadataController.class)
@Import(MetadataControllerAdvice.class)
class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private LogInterceptor logInterceptor;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(metadataService, logInterceptor);
        Mockito.when(logInterceptor.preHandle(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(true);
        objectMapper = JsonMapper.builder().build();
    }

    @Nested
    @DisplayName("GET /api/metadata/units")
    class GetUnits {

        @Test
        @DisplayName("should return list of units")
        void should_return_units_when_units_exist() throws Exception {
            when(metadataService.getUnits()).thenReturn(List.of(new Unit("uri:euros","€")));

            mockMvc.perform(get("/api/metadata/units"))
                    .andExpect(status().isOk());

            verify(metadataService).getUnits();
        }
    }

    @Nested
    @DisplayName("GET /api/metadata/series")
    class GetSeries {

        @Test
        @DisplayName("should return list of series")
        void should_return_series_when_series_exist() throws Exception {
            SerieDto serieDto = SerieDto.builder().id("s1").build();
            when(metadataService.getAllSeries()).thenReturn(List.of(serieDto));

            mockMvc.perform(get("/api/metadata/series"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("s1"));

            verify(metadataService).getAllSeries();
        }
    }

    @Nested
    @DisplayName("GET /api/metadata/series/{id}")
    class GetSerieById {

        @Test
        @DisplayName("should return serie details when id exists")
        void should_return_serie_when_id_exists() throws Exception {
            SerieDto serieDto = SerieDto.builder().id("s1").build();
            when(metadataService.getSerieDetailsById("s1")).thenReturn(serieDto);

            mockMvc.perform(get("/api/metadata/series/{id}", "s1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("s1"));

            verify(metadataService).getSerieDetailsById("s1");
        }

        @Test
        @DisplayName("should return 404 problem detail when serie does not exist")
        void should_return_not_found_when_serie_does_not_exist() throws Exception {
            when(metadataService.getSerieDetailsById("unknown"))
                    .thenThrow(new SerieNotFoundException("Serie unknown not found"));

            mockMvc.perform(get("/api/metadata/series/{id}", "unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Serie Not found"))
                    .andExpect(jsonPath("$.detail").value("Serie unknown not found"))
                    .andExpect(jsonPath("$.instance").value("/api/metadata/series/unknown"));
        }

        @Test
        @DisplayName("should return 500 problem detail when metadata repository fails")
        void should_return_internal_server_error_when_repository_fails() throws Exception {
            when(metadataService.getSerieDetailsById("s1"))
                    .thenThrow(new MetadataRepositoryException("Repository unreachable"));

            mockMvc.perform(get("/api/metadata/series/{id}", "s1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Error when calling metadata repository"))
                    .andExpect(jsonPath("$.detail").value("Repository unreachable"));
        }
    }

    @Nested
    @DisplayName("GET /api/agencies")
    class GetAgencies {

        @Test
        @DisplayName("should return agencies filtered by country when country param is provided")
        void should_return_filtered_agencies_when_country_param_provided() throws Exception {
            AgencyDto agencyDto = AgencyDto.builder().id("fr-insee").build();
            when(metadataService.getAgencies("fr")).thenReturn(List.of(agencyDto));

            mockMvc.perform(get("/api/agencies").param("country", "fr"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("fr-insee"));

            verify(metadataService).getAgencies("fr");
        }

        @Test
        @DisplayName("should return all agencies when no country param is provided")
        void should_return_all_agencies_when_no_country_param() throws Exception {
            AgencyDto agencyDto = AgencyDto.builder().id("fr-insee").build();
            when(metadataService.getAgencies(null)).thenReturn(List.of(agencyDto));

            mockMvc.perform(get("/api/agencies"))
                    .andExpect(status().isOk());

            verify(metadataService).getAgencies(null);
        }
    }

    @Nested
    @DisplayName("POST /api/agencies")
    class PostAgencies {

        @Test
        @DisplayName("should create agency and return created agency")
        void should_create_agency_when_payload_is_valid() throws Exception {
            AgencyDto agencyDto = AgencyDto.builder().id("fr-insee").build();
            when(metadataService.createAgency(any(AgencyDto.class))).thenReturn(agencyDto);

            mockMvc.perform(post("/api/agencies")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(agencyDto))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("fr-insee"));

            verify(metadataService).createAgency(any(AgencyDto.class));
        }

        @Test
        @DisplayName("should return 409 problem detail when agency already exists")
        void should_return_conflict_when_agency_already_exists() throws Exception {
            AgencyDto agencyDto = AgencyDto.builder().id("fr-insee").build();
            when(metadataService.createAgency(any(AgencyDto.class)))
                    .thenThrow(new DDIAgencyAlreadyExists("Agency fr-insee already exists"));

            mockMvc.perform(post("/api/agencies")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(agencyDto))
                            .with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("DDI Agency already exists"))
                    .andExpect(jsonPath("$.detail").value("Agency fr-insee already exists"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/agencies/{id}")
    class DeleteAgencies {

        @Test
        @DisplayName("should delete agency by id")
        void should_delete_agency_when_id_exists() throws Exception {
            mockMvc.perform(delete("/api/agencies/{id}", "fr-insee").with(csrf()))
                    .andExpect(status().isOk());

            verify(metadataService).deleteAgencyById("fr-insee");
        }

        @Test
        @DisplayName("should return 404 problem detail when agency does not exist")
        void should_return_not_found_when_agency_does_not_exist() throws Exception {
            doThrow(new DDIAgencyNotFound("Agency unknown not found"))
                    .when(metadataService).deleteAgencyById("unknown");

            mockMvc.perform(delete("/api/agencies/{id}", "unknown").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("DDI Agency not found"))
                    .andExpect(jsonPath("$.detail").value("Agency unknown not found"));
        }
    }

    @Nested
    @DisplayName("POST /api/metadata/series")
    class PostInternalSerie {

        @Test
        @DisplayName("should create internal serie and return created serie")
        void should_create_internal_serie_when_payload_is_valid() throws Exception {
            SerieDto serieDto = SerieDto.builder().id("s1").build();
            when(metadataService.createInternalSerie(any(SerieDto.class))).thenReturn(serieDto);

            mockMvc.perform(post("/api/metadata/series")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(serieDto))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("s1"));

            verify(metadataService).createInternalSerie(any(SerieDto.class));
        }

        @Test
        @DisplayName("should return 409 problem detail when internal serie already exists")
        void should_return_conflict_when_internal_serie_already_exists() throws Exception {
            SerieDto serieDto = SerieDto.builder().id("s1").build();
            when(metadataService.createInternalSerie(any(SerieDto.class)))
                    .thenThrow(new InternalSerieAlreadyExists("Serie s1 already exists"));

            mockMvc.perform(post("/api/metadata/series")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(serieDto))
                            .with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Internal Serie already exists"))
                    .andExpect(jsonPath("$.detail").value("Serie s1 already exists"));
        }

        @Test
        @DisplayName("should return 400 problem detail when internal serie is invalid")
        void should_return_bad_request_when_internal_serie_is_invalid() throws Exception {
            SerieDto serieDto = SerieDto.builder().id("s1").build();
            when(metadataService.createInternalSerie(any(SerieDto.class)))
                    .thenThrow(new InternalSerieInvalid("Serie s1 is invalid"));

            mockMvc.perform(post("/api/metadata/series")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(serieDto))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Internal Serie invalid"))
                    .andExpect(jsonPath("$.detail").value("Serie s1 is invalid"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/metadata/series/{id}")
    class DeleteInternalSerie {

        @Test
        @DisplayName("should delete internal serie by id")
        void should_delete_internal_serie_when_id_exists() throws Exception {
            mockMvc.perform(delete("/api/metadata/series/{id}", "s1").with(csrf()))
                    .andExpect(status().isOk());

            verify(metadataService).deleteInternalSerieById("s1");
        }

        @Test
        @DisplayName("should return 404 problem detail when internal serie does not exist")
        void should_return_not_found_when_internal_serie_does_not_exist() throws Exception {
            doThrow(new InternalSerieNotFound("Serie unknown not found"))
                    .when(metadataService).deleteInternalSerieById("unknown");

            mockMvc.perform(delete("/api/metadata/series/{id}", "unknown").with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Internal Serie not found"))
                    .andExpect(jsonPath("$.detail").value("Serie unknown not found"));
        }
    }
}