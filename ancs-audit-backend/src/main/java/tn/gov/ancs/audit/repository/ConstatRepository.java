package tn.gov.ancs.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Constat;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConstatRepository extends JpaRepository<Constat, UUID> {

    List<Constat> findByMissionId(UUID missionId);

    List<Constat> findByMissionIdAndResultat(UUID missionId, ResultatConstat resultat);

    Optional<Constat> findByMissionIdAndControleId(UUID missionId, UUID controleId);

    /** Nombre de constats par résultat pour une mission — utilisé pour le taux de conformité. */
    long countByMissionIdAndResultat(UUID missionId, ResultatConstat resultat);

    /** Nombre total de constats par résultat dans toute la base. */
    long countByResultat(ResultatConstat resultat);

    /** Constats non encore synchronisés (mode offline). */
    @Query("SELECT c FROM Constat c WHERE c.synced = false ORDER BY c.createdAt ASC")
    List<Constat> findUnsyncedConstats();

    /**
     * Calcule le taux de conformité d'une mission.
     * Retourne le pourcentage de constats CONFORMES sur le total des constats saisis.
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) = 0 THEN 0.0
               ELSE (SUM(CASE WHEN c.resultat = 'CONFORME' THEN 1 ELSE 0 END) * 100.0 / COUNT(c))
               END
        FROM Constat c WHERE c.mission.id = :missionId
        """)
    Double calculateTauxConformite(UUID missionId);
}
