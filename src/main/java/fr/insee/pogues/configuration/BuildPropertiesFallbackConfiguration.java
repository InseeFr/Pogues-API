package fr.insee.pogues.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Maven {@code build-info} is not generated when the app is started from the IDE.
 */
@Configuration
public class BuildPropertiesFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(BuildProperties.class)
    public BuildProperties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty("group", "fr.insee");
        properties.setProperty("artifact", "Pogues-BO");
        properties.setProperty("name", "Pogues-Back-Office");
        properties.setProperty("version", "dev");
        return new BuildProperties(properties);
    }
}
