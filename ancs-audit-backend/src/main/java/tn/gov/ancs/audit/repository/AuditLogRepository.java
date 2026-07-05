package tn.gov.ancs.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUtilisateurIdOrderByCreatedAtDesc(UUID utilisateurId, Pageable pageable);

    Page<AuditLog> findByResourceAndResourceIdOrderByCreatedAtDesc(
        String resource, UUID resourceId, Pageable pageable);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant debut, Instant fin);
}
