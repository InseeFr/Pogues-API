package fr.insee.pogues.client.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.insee.pogues.client.generation.exceptions.GenerationException;
import fr.insee.pogues.client.generation.model.ByteArrayResourceWithFileName;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationFormat;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import fr.insee.pogues.exception.PoguesException;
import fr.insee.pogues.model.EnoContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class EnoRestClient implements EnoClient {
    private final RestClient restClient;

    public EnoRestClient(@Qualifier("enoApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    private static final String DEFAULT_CONTEXT = "DEFAULT";
    private static final String DEFAULT_CONTEXT_PATH = "questionnaire/" + DEFAULT_CONTEXT;
    private static final String DDI_FILE_NAME = "ddi.xml";
    private static final String POGUES_XML_FILE_NAME = "pogues.xml";
    private static final String POGUES_JSON_FILE_NAME = "pogues.json";
    private static final String PARAMS_FILE_NAME = "params.json";
    private static final String DSFR_QUERY_PARAM = "dsfr";

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    /** {@link EnoClient#getParameters()} */
    @Override
    public void getParameters() {
        String xmlParams = restClient.get()
                .uri("parameters/xml/all")
                .accept(MediaType.ALL)
                .retrieve()
                .body(String.class);
        log.debug("Xml parameters received from the Eno external API:{}{}", System.lineSeparator(), xmlParams);
    }

    @Override
    public GenerationParameters getGenerationParameters(GenerationContext context, GenerationFormat outFormat, CollectMode mode) {
        log.info("EnoClient [GenerationParameters] for context: {}, outFormat: {}, mode: {}", context, outFormat, mode);
        String rawJson = restClient.get()
                .uri("parameters/java/{context}/{outFormat}/{mode}", context, outFormat, mode)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
        return objectMapper.readValue(rawJson, GenerationParameters.class);
    }

    @Override
    public String getPoguesXmlToDDI(String inputAsString) throws GenerationException {
        return callEnoApi(inputAsString, POGUES_XML_FILE_NAME, "/questionnaire/poguesxml-2-ddi");
    }

    @Override
    public String getDDIToODT(String inputAsString) throws GenerationException {
        return callEnoApi(inputAsString, DDI_FILE_NAME, DEFAULT_CONTEXT_PATH + "/fodt");
    }

    @Override
    public String getDDIToFO(String inputAsString) throws GenerationException {
        return callEnoApi(inputAsString, DDI_FILE_NAME, DEFAULT_CONTEXT_PATH + "/fo");
    }

    @Override
    public String getDDIToXForms(String inputAsString) throws GenerationException {
        return callEnoApi(inputAsString, DDI_FILE_NAME, DEFAULT_CONTEXT_PATH + "/xforms");
    }

    @Override
    public String getPoguesJsonToLunaticJson(String inputAsString, Map<String, Object> params) throws GenerationException {
        log.info("EnoClient [PoguesToLunatic]");

        EnoContext context = getContextParam(params);
        String mode = getModeParam(params);
        String wsPath = String.format("questionnaire/pogues-2-lunatic/%s/%s", context, mode);

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(DSFR_QUERY_PARAM, getDsfrParam(params));

        return callEnoApiWithParams(inputAsString, POGUES_JSON_FILE_NAME, wsPath, queryParams);
    }

    @Override
    public String getPoguesJsonToLunaticJson(String inputAsString, GenerationParameters generationParameters) throws GenerationException {
        log.info("EnoClient [PoguesToLunatic] with custom parameters");
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("in", new ByteArrayResourceWithFileName(
                POGUES_JSON_FILE_NAME, inputAsString.getBytes(StandardCharsets.UTF_8)));
        builder.part("params", new ByteArrayResourceWithFileName(
                PARAMS_FILE_NAME, objectMapper.writeValueAsBytes(generationParameters)));
        try {
            byte[] responseBytes = restClient.post()
                    .uri("questionnaire/pogues-2-lunatic")
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(byte[].class);
            if(responseBytes == null) return null;
            return new String(responseBytes, StandardCharsets.UTF_8);
        } catch (RestClientResponseException e) {
            log.error(e.getMessage());
            throw new GenerationException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new GenerationException("Unknown error during generation");
        }
    }

    /** Returns the Eno context from the params map. Default value is the 'DEFAULT' context. */
    static EnoContext getContextParam(Map<String, Object> params) {
        EnoContext enoContext = (EnoContext) params.get("context");
        if (enoContext == null)
            log.warn("null context sent in a Eno request.");
        return (enoContext != null) ? enoContext : EnoContext.HOUSEHOLD;
    }

    static String getModeParam(Map<String, Object> params) {
        Object modePathParam = params.get("mode");
        if (modePathParam == null)
            throw new IllegalStateException("No 'mode' defined in params.");
        return modePathParam.toString();
    }

    static String getDsfrParam(Map<String, Object> params) {
        return Boolean.TRUE.equals(params.get("dsfr")) ? "true" : "false";
    }

    private String callEnoApi(String inputAsString, String fileName, String wsPath) throws GenerationException {
        MultiValueMap<String, String> emptyParams = new LinkedMultiValueMap<>();
        return callEnoApiWithParams(inputAsString, fileName, wsPath, emptyParams);
    }

    private String callEnoApiWithParams(String inputAsString, String fileName, String wsPath, MultiValueMap<String, String> params)
            throws GenerationException, PoguesException {
        URI uri = UriComponentsBuilder
                .fromPath(wsPath)
                .queryParams(params)
                .build().toUri();

        log.info("Call Eno API with URI: {}", uri);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("in", new ByteArrayResourceWithFileName(
                fileName, inputAsString.getBytes(StandardCharsets.UTF_8)));

        try {
            byte[] responseBytes =  restClient.post()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(byte[].class);
            if(responseBytes == null) return null;
            return new String(responseBytes, StandardCharsets.UTF_8);
        } catch (RestClientResponseException e) {
            log.error(e.getMessage());
            throw new GenerationException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new GenerationException("Unknown error during generation");
        }
    }
}
