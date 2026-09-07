package fr.insee.pogues.configuration.properties.visualize;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.stromaev3.vis")
public record WebUiVisualizeProperties(
    String host,
    String path,
    VisualizeQueryParams queryParams
) {
}