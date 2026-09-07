package fr.insee.pogues.persistence.repository;

import fr.insee.pogues.domain.entity.db.Version;

import java.util.List;
import java.util.UUID;

public interface QuestionnaireVersionRepository {

    List<Version> getVersionsByQuestionnaireId(String poguesId, boolean withData);

    Version getLastVersionByQuestionnaireId(String poguesId, boolean withData);

    Version getVersionByVersionId(UUID versionId, boolean withData);

    void createVersion(Version version) throws Exception;

    void cleanVersions();

    void deleteVersionsByQuestionnaireId(String poguesId) throws Exception;

    void deleteAllVersionsByQuestionnaireIdExceptLast(String poguesId) throws Exception;
}
