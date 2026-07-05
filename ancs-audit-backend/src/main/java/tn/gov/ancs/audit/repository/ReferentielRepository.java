package tn.gov.ancs.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Referentiel;
import tn.gov.ancs.audit.domain.enums.TypeReferentiel;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferentielRepository extends JpaRepository<Referentiel, UUID> {

    List<Referentiel> findByType(TypeReferentiel type);

    List<Referentiel> findByNomContainingIgnoreCase(String nom);
}
