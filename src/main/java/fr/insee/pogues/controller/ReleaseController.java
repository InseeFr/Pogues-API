package fr.insee.pogues.controller;


import fr.insee.pogues.configuration.auth.AuthorityPrivileges;
import fr.insee.pogues.configuration.auth.user.UserProvider;
import fr.insee.pogues.model.dto.release.*;
import fr.insee.pogues.service.release.ReleaseRecoveryService;
import fr.insee.pogues.service.release.ReleasePublicationService;
import fr.insee.pogues.service.release.ReleaseWorkflowAsyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaire")
@Tag(name = "15. Release Questionnaire Controller")
@Slf4j
@AllArgsConstructor
public class ReleaseController {

    private static final String POGUES_ID_NULL_ERROR = "Pogues ID cannot be null or empty";

    private final ReleasePublicationService releasePublicationService;
    private final ReleaseRecoveryService releaseRecoveryService;
    private final ReleaseWorkflowAsyncService workflowAsyncService;
    private final UserProvider userProvider;

    @PostMapping("/{poguesId}/releases")
    @Operation(operationId = "createRelease", summary = "Launch Release process", description = "Launch release process in registry API by its Pogues ID.", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReleaseRequestDto.class))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<ReleaseRequestDto> createRelease(
            Authentication authentication,
            @PathVariable String poguesId,
            @RequestBody CreateReleaseRequestDto createReleaseRequestDto) {
        if (poguesId == null || poguesId.trim().isEmpty()) {
            throw new IllegalArgumentException(POGUES_ID_NULL_ERROR);
        }
        String author = userProvider.getUser(authentication).getUserId();
        ReleaseRequestDto releaseRequestDto = releasePublicationService.initReleaseRequest(createReleaseRequestDto, author);

        // launch async publication process
        workflowAsyncService.publishRelease(releaseRequestDto.releaseRequestId());
        log.info("releaseRequestDto : {}", releaseRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(releaseRequestDto);
    }

    @GetMapping("/{poguesId}/release-requests")
    @Operation(operationId = "getPendingReleaseRequests", summary = "Returns list of Release Requests", description = "Returns list of Release Requests by its Pogues ID (not the COMPLETED ones)", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json",  array = @ArraySchema(schema = @Schema(implementation = ReleaseRequestDto.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<ReleaseRequestDto>> getPendingReleaseRequests(@PathVariable String poguesId) {
        if (poguesId == null || poguesId.trim().isEmpty()) {
            throw new IllegalArgumentException(POGUES_ID_NULL_ERROR);
        }
        return ResponseEntity.ok(releaseRecoveryService.getPendingReleaseRequestsByPoguesId(poguesId));
    }



    @GetMapping("/{poguesId}/releases")
    @Operation(operationId = "getReleases", summary = "Returns list of Release COMPLETED in registry", description = "Returns list of Releases by its Pogues ID completed in registry", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json",  array = @ArraySchema(schema = @Schema(implementation = ReleaseDto.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<ReleaseDto>> getReleases(@PathVariable String poguesId) {
        if (poguesId == null || poguesId.trim().isEmpty()) {
            throw new IllegalArgumentException(POGUES_ID_NULL_ERROR);
        }

        List<ReleaseDto> releases = releaseRecoveryService.getCompletedReleasesByPoguesId(poguesId);
        return ResponseEntity.ok(releases);
    }

    @DeleteMapping("/{poguesId}/release-requests/{releaseRequestId}")
    @Operation(operationId = "deleteFailedReleaseRequests", summary = "Delete a FAILED release request", description = "Delete a release request according its ID, only if status is FAILED")
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public void deleteFailedReleaseRequests(
            @PathVariable String poguesId,
            @PathVariable Long releaseRequestId) {
        if (poguesId == null || poguesId.trim().isEmpty()) {
            throw new IllegalArgumentException(POGUES_ID_NULL_ERROR);
        }
        if (releaseRequestId == null) {
            throw new IllegalArgumentException("ReleaseRequestId cannot be null or empty");
        }
        releasePublicationService.deleteFailedReleaseRequestsById(releaseRequestId);
    }
}
