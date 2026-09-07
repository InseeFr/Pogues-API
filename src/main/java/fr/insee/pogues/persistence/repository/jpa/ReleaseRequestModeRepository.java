package fr.insee.pogues.persistence.repository.jpa;

import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReleaseRequestModeRepository extends JpaRepository<ReleaseRequestModeDB, Long> {

    Optional<ReleaseRequestModeDB> findByReleaseRequestIdAndMode(Long releaseRequestId, CollectMode mode);

}
