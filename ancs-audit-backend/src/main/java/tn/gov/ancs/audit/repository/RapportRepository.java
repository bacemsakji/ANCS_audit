package tn.gov.ancs.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Rapport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, UUID> {

    List<Rapport> findByMissionIdOrderByVersionDesc(UUID missionId);

    /** Dernière version du rapport pour une mission. */
    Optional<Rapport> findFirstByMissionIdOrderByVersionDesc(UUID missionId);

    /** Rapport d'un organisme — contrôle d'accès RSSI. */
    @Query("""
        SELECT r FROM Rapport r
        JOIN r.mission m
        WHERE m.organisme.id = :organismeId
        ORDER BY r.dateGeneration DESC
        """)
    List<Rapport> findByOrganismeId(UUID organismeId);

    /** Prochain numéro de version pour une mission. */
    @Query("SELECT COALESCE(MAX(r.version), 0) + 1 FROM Rapport r WHERE r.mission.id = :missionId")
    int getNextVersionForMission(UUID missionId);
}
