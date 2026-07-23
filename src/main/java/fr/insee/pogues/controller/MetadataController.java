package fr.insee.pogues.controller;


import fr.insee.pogues.configuration.auth.AuthorityPrivileges;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import fr.insee.pogues.model.dto.details.QuestionnaireDetailsDto;
import fr.insee.pogues.model.dto.metadata.AgencyDto;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import fr.insee.pogues.service.metadata.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "12. Metadata repository")
@Slf4j
public class MetadataController {

    @Autowired
    MetadataService metadataService;

    @GetMapping("metadata/units")
    @Operation(operationId = "getUnits", summary = "Get units measure", description = "This will give a list of objects containing the uri and the label for all units", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Unit.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<Unit>> getUnits() throws Exception {
        List<Unit> units = metadataService.getUnits();
        return ResponseEntity.status(HttpStatus.OK).body(units);
    }

    @GetMapping("metadata/series")
    @Operation(operationId = "getSeries", summary = "Get all series", description = "This will give a list of series via magma fusion", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SerieDto.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<SerieDto>> getSeries() throws Exception {
        List<SerieDto> series = metadataService.getAllSeries();
        return ResponseEntity.status(HttpStatus.OK).body(series);
    }


    @GetMapping("metadata/series/{id}")
    @Operation(operationId = "getSerieById", summary = "Get details of serie by id", description = "This will give a list of series via magma fusion", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = SerieDto.class))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<SerieDto> getSerieById(
            @PathVariable(value = "id") String id) {
        SerieDto serie = metadataService.getSerieDetailsById(id);
        return ResponseEntity.status(HttpStatus.OK).body(serie);
    }

    @GetMapping("agencies")
    @Operation(operationId = "getAgencies", summary = "Get DDI Agencies", description = "This will give a list Agency of DDI Alliance https://registry.ddialliance.org/Agency", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AgencyDto.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_USER_PRIVILEGES)
    public ResponseEntity<List<AgencyDto>> getAgencies(
            @Parameter(name = "country", in = ParameterIn.QUERY)
            @RequestParam(value = "country", required = false) String countryFilter
    ) {
        List<AgencyDto> agencies = metadataService.getAgencies(countryFilter);
        return ResponseEntity.status(HttpStatus.OK).body(agencies);
    }

    @PostMapping("agencies")
    @Operation(operationId = "postAgencies", summary = "Add DDI Agency", description = "This will add Agency of DDI Alliance to pogues DB", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AgencyDto.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_ADMIN_PRIVILEGES)
    public ResponseEntity<AgencyDto> postAgencies(@RequestBody AgencyDto agencyDto) {
        AgencyDto createdAgency = metadataService.createAgency(agencyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAgency);
    }

    @DeleteMapping("agencies/{id}")
    @Operation(operationId = "deleteAgencies", summary = "Delete DDI Agency", description = "This will delete Agency of DDI Alliance from pogues DB, according to it's id", responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AgencyDto.class)))) })
    @PreAuthorize(AuthorityPrivileges.HAS_ADMIN_PRIVILEGES)
    public void deleteAgencies(@PathVariable(value = "id") String id) {
        metadataService.deleteAgencyById(id);
    }
}
