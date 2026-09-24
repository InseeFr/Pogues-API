package fr.insee.pogues.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "application.registry")
public record RegistryProperties(
        @NestedConfigurationProperty
        RegistryEndpoint questionnaire,

        @NestedConfigurationProperty
        RegistryEndpoint nomenclature
) {

    /**
     * Configuration of registry host.
     *
     * @param host      direct host to registry
     * @param proxyHost proxy host: use if registry need to be exposed to public web with api gateway (ex: Gravitee)
     */
    public record RegistryEndpoint(
            String host,
            String proxyHost
    ) {
        public String resolveProxyHost() {
            return (proxyHost != null && !proxyHost.isBlank()) ? proxyHost : host;
        }
    }
}