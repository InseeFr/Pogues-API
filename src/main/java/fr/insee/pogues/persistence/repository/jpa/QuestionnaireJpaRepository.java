package fr.insee.pogues.persistence.repository.jpa;

import fr.insee.pogues.domain.entity.db.QuestionnaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireJpaRepository extends JpaRepository<QuestionnaireEntity, String> {

    @Query("SELECT DISTINCT q.owner FROM QuestionnaireEntity q WHERE q.owner IS NOT NULL")
    List<String> findDistinctOwners();

    @Query("SELECT q.owner FROM QuestionnaireEntity q WHERE q.id = :id")
    Optional<String> findOwnerById(@Param("id") String id);
}
