package fr.insee.pogues;

import fr.insee.pogues.configuration.PropertiesLogger;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = "fr.insee.pogues")
@EnableTransactionManagement
@ConfigurationPropertiesScan
@Slf4j
public class Pogues extends SpringBootServletInitializer {

	@Value("${application.timezoneId}")
	private String applicationTimeZoneId;

	public static SpringApplicationBuilder configureApplicationBuilder(SpringApplicationBuilder springApplicationBuilder){
		return springApplicationBuilder.sources(Pogues.class).listeners(new PropertiesLogger());
	}

	public static void main(String[] args) {
		if (shouldDefaultToLocalProfile()) {
			System.setProperty("spring.profiles.default", "local");
			log.info("No Spring profile specified, using 'local'");
		}
		configureApplicationBuilder(new SpringApplicationBuilder()).build().run(args);
	}

	/**
	 * IDE / {@code mvn spring-boot:run} put {@code target/classes} on the classpath.
	 * A packaged jar does not, so production is left untouched.
	 */
	static boolean shouldDefaultToLocalProfile() {
		if (hasText(System.getenv("SPRING_PROFILES_ACTIVE"))
				|| hasText(System.getProperty("spring.profiles.active"))
				|| hasText(System.getProperty("spring.profiles.default"))) {
			return false;
		}
		String classpath = System.getProperty("java.class.path", "");
		return classpath.contains("target/classes")
				|| classpath.contains("out/production")
				|| classpath.contains("out\\production");
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	@PostConstruct
	public void executeAfterMain() {
		log.info("Timezone is set to '{}'", applicationTimeZoneId);
		TimeZone.setDefault(TimeZone.getTimeZone(applicationTimeZoneId));
	}

	@EventListener
	public void handleApplicationReady(ApplicationReadyEvent event) {
		log.info("=============== Pogues Back-Office has successfully started. ===============");
	}
}
