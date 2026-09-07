package fr.insee.pogues;

import fr.insee.pogues.configuration.PropertiesLogger;
import fr.insee.pogues.configuration.TimeConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = "fr.insee.pogues")
@EnableTransactionManagement
@ConfigurationPropertiesScan
@Slf4j
public class Pogues extends SpringBootServletInitializer {

	public static SpringApplicationBuilder configureApplicationBuilder(SpringApplicationBuilder springApplicationBuilder){
		return springApplicationBuilder.sources(Pogues.class).listeners(new PropertiesLogger());
	}

	public static void main(String[] args) {
		configureApplicationBuilder(new SpringApplicationBuilder()).build().run(args);
	}

	@PostConstruct
	public void executeAfterMain() {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(TimeConfiguration.UTC)));
	}

	@EventListener
	public void handleApplicationReady(ApplicationReadyEvent event) {
		log.info("=============== Pogues Back-Office has successfully started. ===============");
	}
}
