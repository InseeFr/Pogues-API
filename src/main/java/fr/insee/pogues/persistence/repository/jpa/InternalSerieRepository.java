package fr.insee.pogues.persistence.repository.jpa;

import fr.insee.pogues.domain.entity.db.InternalSerieDB;
import org.springframework.data.jpa.repository.JpaRepository;


public interface InternalSerieRepository extends JpaRepository<InternalSerieDB, String> {
}