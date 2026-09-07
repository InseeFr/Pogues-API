package fr.insee.pogues.persistence.repository.jpa;

import fr.insee.pogues.domain.entity.db.MappingCodesListRegistreDB;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MappingCodesListRegistreRepository
        extends JpaRepository<MappingCodesListRegistreDB, Long> {

    Optional<MappingCodesListRegistreDB> findByPoguesCodesListId(String poguesCodesListId);

    boolean existsByPoguesCodesListId(String poguesCodesListId);

    boolean existsByRegistreCodesListId(UUID registreCodesListId);
}