package fr.insee.pogues.persistence.repository;

import fr.insee.pogues.configuration.cache.CacheName;
import fr.insee.pogues.exception.PoguesException;
import fr.insee.pogues.persistence.exceptions.NonUniqueResultException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Service
public class QuestionnaireRepositoryStub implements QuestionnaireRepository {

	private final Map<String, JsonNode> questionnaires = new HashMap<>();

	public List<JsonNode> getQuestionnaires() {
        return new ArrayList<>(questionnaires.values());
	}

	public JsonNode getQuestionnaireByID(String id) {
		return questionnaires.get(id);
	}

	public Map<String, JsonNode> getQuestionnairesByIds(List<String> ids) {
		Map<String, JsonNode> res = new HashMap<>();
		ids.forEach(id -> {
			JsonNode questionnaire = questionnaires.get(id);
			if (questionnaire != null) { res.put(id, questionnaire); }
		});
		return res;
	}

	public void deleteQuestionnaireByID(String id) {
		questionnaires.remove(id);
	}

	public List<JsonNode> getQuestionnairesByOwner(String owner) {
		List<JsonNode> res = new ArrayList<>();
		questionnaires.forEach((k, v) -> {
			if (Objects.equals(owner, v.get("owner").asText())) { res.add(v); }
		});
		return res;
	}

	public List<JsonNode> getMetaQuestionnaire(String owner) {
		return List.of();
	}

	@Cacheable(CacheName.STAMPS)
	public List<JsonNode> getStamps() throws Exception {
		return List.of();
	}

	public void createQuestionnaire(JsonNode questionnaire) throws Exception {
		String id = questionnaire.get("id").asText();
		if (null != getQuestionnaireByID(id)) {
			throw new NonUniqueResultException("Entity already exists");
		}
		questionnaires.put(id, questionnaire);
	}

	public void updateQuestionnaire(String id, JsonNode questionnaire) {
		questionnaires.put(id, questionnaire);
	}

	/**
	 * A method to count the questionnaires stored in database
	 */
	@Override
	public String countQuestionnaires() {
		int count = questionnaires.size();
		if (count == 0) {
			throw new PoguesException(404, "Not found", "No questionnaires found in database");
		}
		return String.valueOf(count);
	}

}
