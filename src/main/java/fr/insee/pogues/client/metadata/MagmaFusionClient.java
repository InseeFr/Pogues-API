package fr.insee.pogues.client.metadata;

import fr.insee.pogues.client.metadata.exceptions.MetadataRepositoryException;
import fr.insee.pogues.client.metadata.exceptions.SerieNotFoundException;
import fr.insee.pogues.client.metadata.model.magma.fusion.Serie;
import fr.insee.pogues.client.metadata.model.magma.fusion.SerieMetadata;

import java.util.List;

public interface MagmaFusionClient {
    List<Serie> getSeries() throws MetadataRepositoryException, SerieNotFoundException;
    SerieMetadata getSerieById(String serieId) throws MetadataRepositoryException, SerieNotFoundException;
}
