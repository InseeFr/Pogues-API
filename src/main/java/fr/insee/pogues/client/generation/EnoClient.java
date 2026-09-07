package fr.insee.pogues.client.generation;

import fr.insee.pogues.client.generation.exceptions.GenerationException;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationFormat;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;

import java.util.Map;

/**
 * Client with methods to call Eno-WS external web-service.
 */
public interface EnoClient {

	/** Only used as a health-check for the Eno external web-service. */
	void getParameters();

	GenerationParameters getGenerationParameters(GenerationContext context, GenerationFormat outFormat, CollectMode mode);

	String getPoguesXmlToDDI(String inputAsString) throws GenerationException;

	String getDDIToODT (String inputAsString) throws GenerationException;

	String getDDIToFO(String inputAsString) throws GenerationException;

	String getDDIToXForms(String inputAsString) throws GenerationException;

	String getPoguesJsonToLunaticJson(String inputAsString, Map<String, Object> params) throws GenerationException;

	String getPoguesJsonToLunaticJson(String inputAsString, GenerationParameters generationParameters) throws GenerationException;

}
