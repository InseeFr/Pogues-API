package fr.insee.pogues.controller;

import fr.insee.pogues.configuration.log.LogInterceptor;
import fr.insee.pogues.controller.error.ErrorCode;
import fr.insee.pogues.exception.questionnaire.CodesListException;
import fr.insee.pogues.model.dto.codeslists.CodeDTO;
import fr.insee.pogues.model.dto.codeslists.CodesListDTO;
import fr.insee.pogues.model.dto.codeslists.ExtendedCodesListDTO;
import fr.insee.pogues.service.CodesListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "testUser", roles = {"ADMIN"})
@WebMvcTest(CodesListController.class)
class CodesListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CodesListService codesListService;

    @MockitoBean
    private LogInterceptor logInterceptor;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CodesListService codesListService() {
            return Mockito.mock(CodesListService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(codesListService, logInterceptor);
        Mockito.when(logInterceptor.preHandle(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(true);
    }

    @Test
    @DisplayName("Should fetch a single questionnaire codes list")
    void getQuestionnaireCodesList_success() throws Exception {
        // Given a questionnaire with a codes list
        CodesListDTO codesListDTO = new CodesListDTO("m7d794ks", "My codes list", List.of(new CodeDTO("1", "Label 1", null)));
        ExtendedCodesListDTO extendedCodesListDTO = new ExtendedCodesListDTO(codesListDTO, List.of("QUESTION"));
        Mockito.when(codesListService.getQuestionnaireCodesList("my-questionnaire-id", "m7d794ks")).thenReturn(extendedCodesListDTO);
        String expectedJSON = "{\"codes\":[{\"value\":\"1\",\"label\":\"Label 1\"}],\"id\":\"m7d794ks\",\"label\":\"My codes list\",\"relatedQuestionNames\":[\"QUESTION\"]}";

        // When we fetch the codes list
        mockMvc.perform(get("/api/persistence/questionnaire/my-questionnaire-id/codes-list/m7d794ks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 200 and the codes list is returned
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJSON));
    }

    @Test
    @DisplayName("Should return 404 when the codes list doesn't exist")
    void getQuestionnaireCodesList_notFound() throws Exception {
        // Given a questionnaire without the requested codes list
        Mockito.when(codesListService.getQuestionnaireCodesList("my-q-id", "unknown-id"))
                .thenThrow(new CodesListException(404, ErrorCode.CODE_LIST_NOT_FOUND, "Not found",
                        "CodesList with id unknown-id doesn't exist in questionnaire", null));

        // When we fetch a codes list that doesn't exist
        mockMvc.perform(get("/api/persistence/questionnaire/my-q-id/codes-list/unknown-id")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 404
                .andExpect(status().isNotFound());
    }

}
