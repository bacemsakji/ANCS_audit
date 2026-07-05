package tn.gov.ancs.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Mission;
import tn.gov.ancs.audit.domain.enums.StatutMission;

import java.util.List;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    Page<Mission> findByOrganismeId(UUID organismeId, Pageable pageable);

    Page<Mission> findByAuditeurId(UUID auditeurId, Pageable pageable);

    Page<Mission> findByStatut(StatutMission statut, Pageable pageable);

    List<Mission> findByAuditeurIdAndStatut(UUID auditeurId, StatutMission statut);

    List<Mission> findByOrganismeIdAndStatutOrderByCreatedAtDesc(UUID organismeId, StatutMission statut);

    /** Nombre de missions en cours — utilisé par le dashboard admin. */
    long countByStatut(StatutMission statut);

    /** Vérifie que l'auditeur est bien assigné à cette mission (contrôle RBAC). */
    boolean existsByIdAndAuditeurId(UUID missionId, UUID auditeurId);

    /** Vérifie que la mission appartient à l'organisme (contrôle RBAC pour le RSSI). */
    boolean existsByIdAndOrganismeId(UUID missionId, UUID organismeId);

    @Query("""
        SELECT COUNT(m) FROM Mission m
        WHERE m.organisme.id = :organismeId AND m.statut = 'TERMINEE'
        ORDER BY m.createdAt DESC
        """)
    long countTermineesParOrganisme(UUID organismeId);
}
