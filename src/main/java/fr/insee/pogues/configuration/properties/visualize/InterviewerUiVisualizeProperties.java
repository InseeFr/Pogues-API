package fr.insee.pogues.configuration.properties.visualize;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.queen.vis")
public record InterviewerUiVisualizeProperties(
    String host,
    String path,
    VisualizeQueryParams queryParams
) {
}