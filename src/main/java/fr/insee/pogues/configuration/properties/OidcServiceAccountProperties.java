package fr.insee.pogues.configuration.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "feature.oidc.service-account")
public record OidcServiceAccountProperties(
        @NestedConfigurationProperty
        OidcServiceAccount metadata,
        @NestedConfigurationProperty
        OidcServiceAccount questionnaireRegistry){

    public record OidcServiceAccount(
            long refreshMargin,
            String authServerUrl,
            String realm,
            String clientId,
            String clientSecret
    ){}
}
