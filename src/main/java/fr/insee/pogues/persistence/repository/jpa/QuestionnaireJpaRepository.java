package fr.insee.pogues.persistence.repository.jpa;

import fr.insee.pogues.domain.entity.db.QuestionnaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireJpaRepository extends JpaRepository<QuestionnaireEntity, String> {
}
