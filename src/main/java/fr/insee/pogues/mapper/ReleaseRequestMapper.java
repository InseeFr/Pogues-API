package fr.insee.pogues.mapper;


import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import fr.insee.pogues.model.dto.release.CreateReleaseRequestDto;
import fr.insee.pogues.model.dto.release.OverrideGenerationParameters;
import fr.insee.pogues.model.dto.release.ReleaseRequestDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class ReleaseRequestMapper {

    public ReleaseRequestDto toDto(ReleaseRequestDB entity){
        if(entity == null) return null;

        return new ReleaseRequestDto(
                entity.getId(),
                entity.getRequestedBy(),
                entity.getRequestedAt(),
                entity.getStatus(),
                entity.getStatusDescription() != null ? entity.getStatusDescription().label : null,
                entity.getPoguesVersionId(),
                entity.getPoguesId(),
                entity.getReleaseDescription(),
                entity.getModes() != null ? entity.getModes().stream().map(ReleaseRequestModeDB::getMode).toList() : null,
                entity.getContext(),
                new OverrideGenerationParameters(entity.getGenerationParamsQuestionNumberingMode(), entity.getGenerationParamsResponseTimeQuestion())
        );
    }

}
