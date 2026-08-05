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

    /**
     * Checks whether the given domain is covered by an existing entry in the table,
     * either as an exact match or as a sub-domain of it.
     * <p>
     * Domains are stored in reverse notation (e.g. "com.example"). A domain is considered
     * covered if it equals a stored entry, or if it starts with a stored entry followed by a dot
     * (e.g. "com.example.sub" is covered by "com.example", but "com.examples" is not).
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM ddi_agency
            WHERE name = :domain
               OR :domain LIKE name || '.%'
        )
        """, nativeQuery = true)
    boolean existsMatchingDomain(@Param("domain") String domain);

    @Modifying
    @Query("DELETE FROM DDIAgencyDB a WHERE a.name = :name")
    int deleteByName(@Param("name") String name);
}