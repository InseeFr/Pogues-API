package fr.insee.pogues.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfiguration {

    public static final String UTC = "UTC";

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
