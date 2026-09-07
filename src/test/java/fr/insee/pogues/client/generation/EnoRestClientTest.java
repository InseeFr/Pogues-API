package fr.insee.pogues.client.generation;

import fr.insee.pogues.model.EnoContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnoRestClientTest {


    @Test
    void getContextParam_withContextKey() {
        Map<String, Object> params = Map.of("context", EnoContext.BUSINESS);
        EnoContext result = EnoRestClient.getContextParam(params);
        assertEquals(EnoContext.BUSINESS, result);
    }

    @Test
    void getContextParam_withoutContextKey() {
        Map<String, Object> params = new HashMap<>();
        EnoContext result = EnoRestClient.getContextParam(params);
        assertEquals(EnoContext.HOUSEHOLD, result);
    }

    @Test
    void getContextParam_withIncorrectContextKey() {
        Map<String, Object> params = Map.of("notAContext", "notAValue");
        EnoContext result = EnoRestClient.getContextParam(params);
        assertEquals(EnoContext.HOUSEHOLD, result);
    }

    @Test
    void getModeParam_withModeKey() {
        Map<String, Object> params = Map.of("mode", "CAPI");
        String mode = EnoRestClient.getModeParam(params);
        assertEquals("CAPI", mode);
    }

    @Test
    void getModeParam_withoutModeKey() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> EnoRestClient.getModeParam(params));
    }

    @Test
    void getDsfrParam_true() {
        Map<String, Object> params = Map.of("dsfr", true);
        String value = EnoRestClient.getDsfrParam(params);
        assertEquals("true", value);
    }

    @Test
    void getDsfrParam_false() {
        Map<String, Object> params = Map.of("dsfr", false);
        String value = EnoRestClient.getDsfrParam(params);
        assertEquals("false", value);
    }

    @Test
    void getDsfrParam_defaultValue() {
        Map<String, Object> params = new HashMap<>();
        String value = EnoRestClient.getDsfrParam(params);
        assertEquals("false", value);
    }
}
