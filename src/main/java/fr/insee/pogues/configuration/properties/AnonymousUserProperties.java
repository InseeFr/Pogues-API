package fr.insee.pogues.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feature.anonymous-user")
public record AnonymousUserProperties(
        String stamp,
        String name,
        String id
) {
}
