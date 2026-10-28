package fr.insee.pogues.controller;

import fr.insee.pogues.configuration.log.LogInterceptor;
import fr.insee.pogues.exception.loops.LoopInvalidException;
import fr.insee.pogues.exception.loops.LoopNotFoundException;
import fr.insee.pogues.exception.loops.LoopUsedAsBasedOnException;
import fr.insee.pogues.model.dto.loops.ExtendedLoopDTO;
import fr.insee.pogues.model.dto.loops.LoopDTO;
import fr.insee.pogues.model.dto.loops.LoopInitialMemberDTO;
import fr.insee.pogues.model.dto.loops.LoopMemberDTO;
import fr.insee.pogues.model.dto.loops.LoopMemberTypeDTOEnum;
import fr.insee.pogues.service.LoopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "testUser", roles = {"ADMIN"})
@WebMvcTest(LoopController.class)
class LoopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoopService loopService;

    @MockitoBean
    private LogInterceptor logInterceptor;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public LoopService loopService() {
            return Mockito.mock(LoopService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(loopService, logInterceptor);
        Mockito.when(logInterceptor.preHandle(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(true);
    }

    @Test
    @DisplayName("Should fetch questionnaire loop members")
    void getQuestionnaireLoopMembers_success() throws Exception {
        // Given a questionnaire with loop members
        LoopInitialMemberDTO initialMember = new LoopInitialMemberDTO("S1", "S1_NAME", LoopMemberTypeDTOEnum.SEQUENCE, List.of(
                new LoopMemberDTO("S1", "S1_NAME", LoopMemberTypeDTOEnum.SEQUENCE),
                new LoopMemberDTO("E1", "E1_NAME", LoopMemberTypeDTOEnum.EXTERNAL_ELEMENT)));
        Mockito.when(loopService.getQuestionnaireLoopMembers("my-q-id")).thenReturn(List.of(initialMember));
        String expectedJSON = "[{\"finalMembers\":[{\"id\":\"S1\",\"name\":\"S1_NAME\",\"type\":\"SEQUENCE\"},{\"id\":\"E1\",\"name\":\"E1_NAME\",\"type\":\"EXTERNAL_ELEMENT\"}],\"id\":\"S1\",\"name\":\"S1_NAME\",\"type\":\"SEQUENCE\"}]";

        // When we fetch the questionnaire loop members
        mockMvc.perform(get("/api/questionnaires/my-q-id/loops/members")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 200 and the loop members are returned
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJSON));
    }

    @Test
    @DisplayName("Should fetch the loop members of a questionnaire's version")
    void getQuestionnaireVersionLoopMembers_success() throws Exception {
        // Given a questionnaire's version with loop members
        UUID versionId = UUID.randomUUID();
        LoopInitialMemberDTO initialMember = new LoopInitialMemberDTO("SS1", "SS1_NAME", LoopMemberTypeDTOEnum.SUBSEQUENCE, List.of(
                new LoopMemberDTO("SS1", "SS1_NAME", LoopMemberTypeDTOEnum.SUBSEQUENCE)));
        Mockito.when(loopService.getVersionLoopMembers(versionId)).thenReturn(List.of(initialMember));
        String expectedJSON = "[{\"finalMembers\":[{\"id\":\"SS1\",\"name\":\"SS1_NAME\",\"type\":\"SUBSEQUENCE\"}],\"id\":\"SS1\",\"name\":\"SS1_NAME\",\"type\":\"SUBSEQUENCE\"}]";

        // When we fetch the loop members of the version
        mockMvc.perform(get("/api/questionnaires/my-q-id/version/" + versionId + "/loops/members")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 200 and the loop members are returned (and not a loop with the id "members")
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJSON));
        Mockito.verify(loopService, Mockito.never()).getVersionLoop(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Should fetch a loop")
    void getQuestionnaireLoop_success() throws Exception {
        // Given a loop with a dynamic number of occurrences
        ExtendedLoopDTO loop = new ExtendedLoopDTO(
                new LoopDTO("L1", "BOUCLE", "S1", "S2", null, null, false, null, null, "1", "5"), List.of("BOUCLE2"));
        Mockito.when(loopService.getQuestionnaireLoop("my-q-id", "L1")).thenReturn(loop);
        String expectedJSON = "{\"id\":\"L1\",\"name\":\"BOUCLE\",\"initialMember\":\"S1\",\"finalMember\":\"S2\",\"isFixedLength\":false,\"minimum\":\"1\",\"maximum\":\"5\",\"relatedLoopNames\":[\"BOUCLE2\"]}";

        // When we fetch the loop
        mockMvc.perform(get("/api/questionnaires/my-q-id/loops/L1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 200 and the loop in the format used to post it with its related loops, without the null fields
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJSON))
                .andExpect(jsonPath("$.loop").doesNotExist())
                .andExpect(jsonPath("$.basedOn").doesNotExist())
                .andExpect(jsonPath("$.size").doesNotExist());
    }

    @Test
    @DisplayName("Should send back a 404 when the loop does not exist")
    void getQuestionnaireLoop_notFound() throws Exception {
        Mockito.when(loopService.getQuestionnaireLoop("my-q-id", "L1"))
                .thenThrow(new LoopNotFoundException("L1", "my-q-id"));

        mockMvc.perform(get("/api/questionnaires/my-q-id/loops/L1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Loop not found"));
    }

    @Test
    @DisplayName("Should fetch a loop of a questionnaire's version")
    void getQuestionnaireVersionLoop_success() throws Exception {
        // Given a questionnaire's version with a loop based on another one
        UUID versionId = UUID.randomUUID();
        ExtendedLoopDTO loop = new ExtendedLoopDTO(
                new LoopDTO("L2", "BOUCLE2", "S1", "S1", "L1", "true", null, null, null, null, null), List.of());
        Mockito.when(loopService.getVersionLoop(versionId, "L2")).thenReturn(loop);
        String expectedJSON = "{\"id\":\"L2\",\"name\":\"BOUCLE2\",\"initialMember\":\"S1\",\"finalMember\":\"S1\",\"basedOn\":\"L1\",\"filter\":\"true\",\"relatedLoopNames\":[]}";

        // When we fetch the loop of the version
        mockMvc.perform(get("/api/questionnaires/my-q-id/version/" + versionId + "/loops/L2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 200 and the loop in the format used to post it with its related loops
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJSON));
    }

    @Test
    @DisplayName("Should send back a 404 when the loop does not exist in the questionnaire's version")
    void getQuestionnaireVersionLoop_notFound() throws Exception {
        UUID versionId = UUID.randomUUID();
        Mockito.when(loopService.getVersionLoop(versionId, "L1"))
                .thenThrow(new LoopNotFoundException("L1", "my-q-id"));

        mockMvc.perform(get("/api/questionnaires/my-q-id/version/" + versionId + "/loops/L1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Loop not found"));
    }

    @Test
    @DisplayName("Should create a loop")
    void upsertQuestionnaireLoop_created() throws Exception {
        // Given a loop which does not exist yet
        String body = "{\"id\":\"L1\",\"name\":\"BOUCLE\",\"initialMember\":\"S1\",\"finalMember\":\"S2\",\"isFixedLength\":true,\"size\":\"3\",\"shouldSplitIterations\":false}";
        LoopDTO expectedLoop = new LoopDTO("L1", "BOUCLE", "S1", "S2", null, null, true, "3", false, null, null);
        Mockito.when(loopService.upsertQuestionnaireLoop("my-q-id", expectedLoop)).thenReturn(true);

        // When we post the loop
        mockMvc.perform(post("/api/questionnaires/my-q-id/loop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 201
                .andExpect(status().isCreated());
        Mockito.verify(loopService).upsertQuestionnaireLoop("my-q-id", expectedLoop);
    }

    @Test
    @DisplayName("Should update a loop")
    void upsertQuestionnaireLoop_updated() throws Exception {
        // Given a loop which already exists
        String body = "{\"id\":\"L1\",\"name\":\"BOUCLE\",\"initialMember\":\"S1\",\"finalMember\":\"S2\",\"basedOn\":\"TABLE\",\"filter\":\"$AGE$ >= 18\"}";
        LoopDTO expectedLoop = new LoopDTO("L1", "BOUCLE", "S1", "S2", "TABLE", "$AGE$ >= 18", null, null, null, null, null);
        Mockito.when(loopService.upsertQuestionnaireLoop("my-q-id", expectedLoop)).thenReturn(false);

        // When we post the loop
        mockMvc.perform(post("/api/questionnaires/my-q-id/loop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 200
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should send back a 400 when mandatory fields are missing or the name is invalid")
    void upsertQuestionnaireLoop_badRequest() throws Exception {
        // When we post a loop without id and members, and with an invalid name
        mockMvc.perform(post("/api/questionnaires/my-q-id/loop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MA BOUCLE\",\"isFixedLength\":true,\"size\":\"3\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 400 listing the invalid fields, and the service is not called
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(4));
        Mockito.verifyNoInteractions(loopService);
    }

    @Test
    @DisplayName("Should send back a 400 when the loop is invalid")
    void upsertQuestionnaireLoop_invalid() throws Exception {
        // Given a loop whose name is already used
        Mockito.when(loopService.upsertQuestionnaireLoop(Mockito.eq("my-q-id"), Mockito.any()))
                .thenThrow(new LoopInvalidException("The name BOUCLE is already used in the questionnaire"));

        // When we post the loop
        mockMvc.perform(post("/api/questionnaires/my-q-id/loop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"L1\",\"name\":\"BOUCLE\",\"initialMember\":\"S1\",\"finalMember\":\"S1\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 400 with the reason
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The name BOUCLE is already used in the questionnaire"));
    }

    @Test
    @DisplayName("Should delete a loop")
    void deleteQuestionnaireLoop_success() throws Exception {
        // When we delete the loop
        mockMvc.perform(delete("/api/questionnaires/my-q-id/loops/L1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // Then we receive a 204
                .andExpect(status().isNoContent());
        Mockito.verify(loopService).deleteQuestionnaireLoop("my-q-id", "L1");
    }

    @Test
    @DisplayName("Should send back a 404 when deleting a loop which does not exist")
    void deleteQuestionnaireLoop_notFound() throws Exception {
        Mockito.doThrow(new LoopNotFoundException("L1", "my-q-id"))
                .when(loopService).deleteQuestionnaireLoop("my-q-id", "L1");

        mockMvc.perform(delete("/api/questionnaires/my-q-id/loops/L1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Loop not found"));
    }

    @Test
    @DisplayName("Should send back a 409 when deleting a loop on which other loops are based")
    void deleteQuestionnaireLoop_usedAsBasedOn() throws Exception {
        Mockito.doThrow(new LoopUsedAsBasedOnException("BOUCLE", List.of("BOUCLE2")))
                .when(loopService).deleteQuestionnaireLoop("my-q-id", "L1");

        mockMvc.perform(delete("/api/questionnaires/my-q-id/loops/L1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Loop used as based on"))
                .andExpect(jsonPath("$.detail").value("The loop BOUCLE cannot be deleted because the following loops are based on it: BOUCLE2"));
    }

}
