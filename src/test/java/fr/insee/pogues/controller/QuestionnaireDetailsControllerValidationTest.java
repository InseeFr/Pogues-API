package fr.insee.pogues.controller;

import fr.insee.pogues.configuration.log.LogInterceptor;
import fr.insee.pogues.configuration.properties.ApplicationProperties;
import fr.insee.pogues.exception.details.QuestionnaireDetailsInvalidException;
import fr.insee.pogues.model.SurveyModeEnum;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.service.QuestionnaireDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "testUser", roles = {"ADMIN"})
@WebMvcTest(QuestionnaireDetailsController.class)
class QuestionnaireDetailsControllerValidationTest {

    private static final String VALID_DETAILS = """
            {"id":"m5noru0h","name":"ENQLOG2026","label":"Enquête logement 2026","targetMode":["CAWI"],
            "agency":"fr.insee","owner":"DR59-SNDI59"}""";

    /** Blank label and owner, no target mode, no agency. */
    private static final String INVALID_DETAILS = """
            {"id":"m5noru0h","name":"ENQLOG2026","label":" ","targetMode":[],"owner":""}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionnaireDetailsService questionnaireDetailsService;

    @MockitoBean
    private LogInterceptor logInterceptor;

    @MockitoBean
    private ApplicationProperties applicationProperties;

    @BeforeEach
    void resetMocks() {
        Mockito.when(logInterceptor.preHandle(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
    }

    @Test
    @DisplayName("Should create a questionnaire when details are valid")
    void createQuestionnaireFromDetails_success() throws Exception {
        Mockito.when(questionnaireDetailsService.createQuestionnaireFromDetails(any()))
                .thenReturn(new QuestionnaireDetailsDto("m5noru0h", "ENQLOG2026", "Enquête logement 2026", null,
                        List.of(SurveyModeEnum.CAWI), "fr.insee", "DR59-SNDI59"));

        mockMvc.perform(post("/api/persistence/questionnaires/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DETAILS)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("m5noru0h"));
    }

    @Test
    @DisplayName("Should send back a 400 on creation when details are invalid")
    void createQuestionnaireFromDetails_badRequest() throws Exception {
        mockMvc.perform(post("/api/persistence/questionnaires/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_DETAILS)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
        Mockito.verifyNoInteractions(questionnaireDetailsService);
    }

    @Test
    @DisplayName("Should update details when they are valid")
    void updateQuestionnaireDetails_success() throws Exception {
        mockMvc.perform(put("/api/persistence/questionnaire/m5noru0h/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DETAILS)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        Mockito.verify(questionnaireDetailsService).updateQuestionnaireDetailsById(eq("m5noru0h"), any());
    }

    @Test
    @DisplayName("Should send back a 400 on update when details are invalid, as on creation")
    void updateQuestionnaireDetails_badRequest() throws Exception {
        mockMvc.perform(put("/api/persistence/questionnaire/m5noru0h/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_DETAILS)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
        Mockito.verifyNoInteractions(questionnaireDetailsService);
    }

    @Test
    @DisplayName("Should send back a 400 when serie or agency are rejected by the service")
    void createQuestionnaireFromDetails_invalidMetadata() throws Exception {
        Mockito.when(questionnaireDetailsService.createQuestionnaireFromDetails(any()))
                .thenThrow(new QuestionnaireDetailsInvalidException("DDI Agency \"fr.insee\" doesn't exist"));

        mockMvc.perform(post("/api/persistence/questionnaires/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DETAILS)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid questionnaire details"))
                .andExpect(jsonPath("$.detail").value("DDI Agency \"fr.insee\" doesn't exist"));
    }

}
