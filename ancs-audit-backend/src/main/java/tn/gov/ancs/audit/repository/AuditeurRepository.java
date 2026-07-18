package tn.gov.ancs.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Auditeur;
import tn.gov.ancs.audit.domain.enums.StatutAuditeur;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditeurRepository extends JpaRepository<Auditeur, UUID> {

    Optional<Auditeur> findByUtilisateurId(UUID utilisateurId);

    Optional<Auditeur> findByNumeroCertification(String numeroCertification);

    Page<Auditeur> findByStatut(StatutAuditeur statut, Pageable pageable);

    /**
     * Auditeurs dont la certification expire dans les {@code joursAvantExpiration} prochains jours.
     * Utilisé par le scheduler d'alertes.
     */
    @Query("SELECT a FROM Auditeur a WHERE a.statut = 'ACTIF' AND a.dateExpiration <= :dateLimite")
    List<Auditeur> findActifsExpirantAvant(LocalDate dateLimite);

    boolean existsByNumeroCertification(String numeroCertification);

    /**
     * Nombre d'auditeurs ayant un statut donné.
     * Utilisé par le dashboard admin pour afficher le nombre d'auditeurs actifs.
     */
    long countByStatut(StatutAuditeur statut);
}
