package fr.insee.pogues.client.metadata.mock;

import fr.insee.pogues.client.metadata.MagmaFusionClient;
import fr.insee.pogues.client.metadata.exceptions.MetadataRepositoryException;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.client.metadata.model.magma.fusion.Label;
import fr.insee.pogues.client.metadata.model.magma.fusion.Operation;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "feature.metadata.magma-client", havingValue = "mock")
public class MagmaFusionMockClient implements MagmaFusionClient {

    private final Serie mockSerie = new Serie(
            "s1",
            "http://id.insee.fr/operations/serie/s1",
            List.of(
                    new Label("Simple série", "fr"),
                    new Label("Simple serie", "en"))
    );
    @Override
    public List<Serie> getSeries() throws MetadataRepositoryException, SerieNotFoundException {
        return List.of(mockSerie);
    }

    @Override
    public SerieMetadata getSerieById(String serieId) throws MetadataRepositoryException, SerieNotFoundException {
        if(!mockSerie.seriesId().equals(serieId)) throw new SerieNotFoundException(String.format("Serie (id:%s) not found", serieId));
        return new SerieMetadata(
                mockSerie.seriesId(),
                mockSerie.uri(),
                mockSerie.label(),
                List.of(
                        new Label("SIMPLE_SERIE", "fr"),
                        new Label("SIMPLE_SERIE", "en")),
                List.of(
                        new Operation("s11","http://id.insee.fr/operations/operation/s11",
                                List.of(
                                        new Label("Operation de la série 'Simple série'", "fr"),
                                        new Label("Operation of the serie 'Simple serie'", "en")))
                )
        );
    }
}
