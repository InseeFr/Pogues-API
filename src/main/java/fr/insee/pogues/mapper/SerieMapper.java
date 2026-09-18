package fr.insee.pogues.mapper;

import fr.insee.pogues.client.metadata.model.magma.fusion.Label;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import fr.insee.pogues.domain.entity.db.InternalSerieDB;
import fr.insee.pogues.model.dto.metadata.SerieDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SerieMapper {

    private static final String FR_LANGUAGE = "fr";

    private String findFrLabel(List<Label> labels) {
        Optional<Label> labelOptional = labels.stream()
                .filter(label -> FR_LANGUAGE.equals(label.langue()))
                .findFirst();

        if (labelOptional.isPresent()) return labelOptional.get().contenu();
        return "";
    }

    public SerieDto toDto(InternalSerieDB internalSerie){
        if(internalSerie == null) return null;

        return new SerieDto(
                internalSerie.getId(),
                internalSerie.getUri(),
                internalSerie.getLabel(),
                internalSerie.getAltLabel());
    }

    public SerieDto toDto(Serie rmesSerie){
        if(rmesSerie == null) return null;

        return new SerieDto(
                rmesSerie.seriesId(),
                rmesSerie.uri(),
                findFrLabel(rmesSerie.label()),
                null
        );
    }

    public SerieDto toDto(SerieMetadata serieMetadata){
        if(serieMetadata == null) return null;

        return new SerieDto(
                serieMetadata.seriesId(),
                serieMetadata.uri(),
                findFrLabel(serieMetadata.label()),
                findFrLabel(serieMetadata.altLabel()));
    }

}
