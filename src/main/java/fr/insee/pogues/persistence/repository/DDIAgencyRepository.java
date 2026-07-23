package fr.insee.pogues.persistence.repository;

import fr.insee.pogues.domain.entity.db.DDIAgencyDB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DDIAgencyRepository extends JpaRepository<DDIAgencyDB, Long> {
    List<DDIAgencyDB> findByNameStartingWith(String country);
    boolean existsByName(String name);
    @Modifying
    @Query("DELETE FROM DDIAgencyDB a WHERE a.name = :name")
    int deleteByName(@Param("name") String name);
}