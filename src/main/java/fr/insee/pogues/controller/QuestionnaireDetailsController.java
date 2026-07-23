package fr.insee.pogues.controller;


import fr.insee.pogues.configuration.auth.AuthorityPrivileges;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.service.QuestionnaireDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/persistence")
@Tag(name = "05. Questionnaire details Controller")
@Slf4j
@AllArgsConstructor
public class QuestionnaireDetailsController {

    private final QuestionnaireDetailsService questionnaireDetailsService;


    @GetMapping("/questionnaire/{poguesId}/details")
    @Operation(operationId = "getQuestionnaireDetails", summary = "Get details of Questionnaire", description = "This will give all details of Questionnaire", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuestionnaireDetailsDto.class))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<QuestionnaireDetailsDto> getQuestionnaireDetails(
            @PathVariable(value = "poguesId") String poguesId) throws Exception {
        QuestionnaireDetailsDto questionnaireDetails = questionnaireDetailsService.getQuestionnaireDetailsById(poguesId);
        return ResponseEntity.status(HttpStatus.OK).body(questionnaireDetails);
    }


    @GetMapping("/questionnaire/{poguesId}/version/{versionId}/details")
    @Operation(operationId = "getQuestionnaireVersionDetails", summary = "Get details from a questionnaire's backup", description = "This will give all details of questionnaire's backup", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuestionnaireDetailsDto.class))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<QuestionnaireDetailsDto> getQuestionnaireVersionDetails(
            @PathVariable(value = "poguesId") String ignoredQuestionnaireId,
            @PathVariable(value = "versionId") UUID versionId) throws Exception {
        QuestionnaireDetailsDto questionnaireDetails = questionnaireDetailsService.getQuestionnaireDetailsByVersionId(versionId);
        return ResponseEntity.status(HttpStatus.OK).body(questionnaireDetails);
    }


    @PutMapping("/questionnaire/{poguesId}/details")
    @Operation(operationId = "updateQuestionnaireDetails", summary = "Update details of a questionnaire", description = "This will update all details of questionnaire")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public void updateQuestionnaireDetails(
            @PathVariable(value = "poguesId") String poguesId,
            @RequestBody QuestionnaireDetailsDto questionnaireDetailsDto) throws Exception {
            questionnaireDetailsService.updateQuestionnaireDetailsById(poguesId, questionnaireDetailsDto);
    }

}
