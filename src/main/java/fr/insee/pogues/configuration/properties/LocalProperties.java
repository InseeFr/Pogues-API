package fr.insee.pogues.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.local")
public record LocalProperties(
        String questionnairesDirectory
) {
}
