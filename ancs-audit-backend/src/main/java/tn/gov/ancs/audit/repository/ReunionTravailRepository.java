package tn.gov.ancs.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.ReunionTravail;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les réunions de travail d'une mission d'audit.
 *
 * <p>Les réunions de travail et leurs PV doivent être transmis à l'ANCS
 * avec le rapport d'audit (décret 2004-1250, délai de 10 jours).
 */
@Repository
public interface ReunionTravailRepository extends JpaRepository<ReunionTravail, UUID> {

    /**
     * Récupère toutes les réunions de travail d'une mission,
     * triées par date de réunion croissante.
     *
     * @param missionId identifiant de la mission
     * @return liste ordonnée des réunions de travail
     */
    List<ReunionTravail> findByMissionIdOrderByDateReunionAsc(UUID missionId);

    /**
     * Compte le nombre de réunions de travail liées à une mission.
     * Utile pour vérifier qu'au moins une réunion a été tenue avant soumission.
     *
     * @param missionId identifiant de la mission
     * @return nombre de réunions de travail
     */
    long countByMissionId(UUID missionId);
}
