package fr.insee.pogues.controller;

import fr.insee.pogues.configuration.auth.AuthorityPrivileges;
import fr.insee.pogues.controller.error.ApiMessage;
import fr.insee.pogues.model.dto.loops.ExtendedLoopDTO;
import fr.insee.pogues.model.dto.loops.LoopDTO;
import fr.insee.pogues.model.dto.loops.LoopInitialMemberDTO;
import fr.insee.pogues.service.LoopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * WebService class used to fetch and update the loops of a questionnaire.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "16. Loop Controller")
@Slf4j
public class LoopController {

    private final LoopService loopService;

    public LoopController(LoopService loopService) {
        this.loopService = loopService;
    }

    @Operation(summary = "Get the components that can be used as initial and final members of a loop, used for pogues frontend")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LoopInitialMemberDTO.class))) }),
            @ApiResponse(responseCode = "404", description = "Questionnaire not found", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }) })
    @GetMapping("/questionnaires/{questionnaireId}/loops/members")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<LoopInitialMemberDTO>> getQuestionnaireLoopMembers(
            @PathVariable(value = "questionnaireId") String questionnaireId
    ) throws Exception {
        List<LoopInitialMemberDTO> loopMembers = loopService.getQuestionnaireLoopMembers(questionnaireId);
        return ResponseEntity.status(HttpStatus.OK).body(loopMembers);
    }

    @Operation(summary = "Get the components of a questionnaire's backup that can be used as initial and final members of a loop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LoopInitialMemberDTO.class))) }),
            @ApiResponse(responseCode = "404", description = "Version not found", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }) })
    @GetMapping("/questionnaires/{questionnaireId}/version/{versionId}/loops/members")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<LoopInitialMemberDTO>> getQuestionnaireVersionLoopMembers(
            @PathVariable(value = "questionnaireId") String ignoredQuestionnaireId,
            @PathVariable(value = "versionId") UUID versionId
    ) throws Exception {
        List<LoopInitialMemberDTO> loopMembers = loopService.getVersionLoopMembers(versionId);
        return ResponseEntity.status(HttpStatus.OK).body(loopMembers);
    }

    @Operation(summary = "Get a loop of a questionnaire, in the format used to create or update it")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ExtendedLoopDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Questionnaire or loop not found", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }) })
    @GetMapping("/questionnaires/{questionnaireId}/loops/{loopId}")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<ExtendedLoopDTO> getQuestionnaireLoop(
            @PathVariable(value = "questionnaireId") String questionnaireId,
            @PathVariable(value = "loopId") String loopId
    ) throws Exception {
        ExtendedLoopDTO loop = loopService.getQuestionnaireLoop(questionnaireId, loopId);
        return ResponseEntity.status(HttpStatus.OK).body(loop);
    }

    @Operation(summary = "Get a loop of a questionnaire's backup, in the format used to create or update it")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ExtendedLoopDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Version or loop not found", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }) })
    @GetMapping("/questionnaires/{questionnaireId}/version/{versionId}/loops/{loopId}")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<ExtendedLoopDTO> getQuestionnaireVersionLoop(
            @PathVariable(value = "questionnaireId") String ignoredQuestionnaireId,
            @PathVariable(value = "versionId") UUID versionId,
            @PathVariable(value = "loopId") String loopId
    ) throws Exception {
        ExtendedLoopDTO loop = loopService.getVersionLoop(versionId, loopId);
        return ResponseEntity.status(HttpStatus.OK).body(loop);
    }

    @Operation(summary = "Update or create a loop in a questionnaire, and update the scope of the collected variables accordingly")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid loop", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }),
            @ApiResponse(responseCode = "404", description = "Questionnaire not found", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }) })
    @PostMapping("/questionnaires/{questionnaireId}/loop")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<Void> upsertQuestionnaireLoop(
            @PathVariable(value = "questionnaireId") String questionnaireId,
            @Valid @RequestBody LoopDTO loop
    ) throws Exception {
        boolean isCreated = loopService.upsertQuestionnaireLoop(questionnaireId, loop);
        if (isCreated) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Delete a loop from a questionnaire, and update the scope of the collected variables accordingly")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Questionnaire or loop not found", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }),
            @ApiResponse(responseCode = "409", description = "Other loops are based on the loop", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ApiMessage.class)) }) })
    @DeleteMapping("/questionnaires/{questionnaireId}/loops/{loopId}")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<Void> deleteQuestionnaireLoop(
            @PathVariable(value = "questionnaireId") String questionnaireId,
            @PathVariable(value = "loopId") String loopId
    ) throws Exception {
        loopService.deleteQuestionnaireLoop(questionnaireId, loopId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
