package fr.insee.pogues.persistence.repository.jpa;

import fr.insee.pogues.domain.entity.db.ReleaseRequestDB;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReleaseRequestRepository extends JpaRepository<ReleaseRequestDB, Long> {

    boolean existsByPoguesVersionId(UUID poguesVersionId);

    @EntityGraph(attributePaths = "modes")
    List<ReleaseRequestDB> findWithModesByPoguesIdAndStatusNot(String poguesId, ReleaseRequestStatus status);

    @EntityGraph(attributePaths = "modes")
    List<ReleaseRequestDB> findWithModesByPoguesIdAndStatus(String poguesId, ReleaseRequestStatus status);

    @EntityGraph(attributePaths = "modes")
    Optional<ReleaseRequestDB> findWithModesById(Long id);

}
