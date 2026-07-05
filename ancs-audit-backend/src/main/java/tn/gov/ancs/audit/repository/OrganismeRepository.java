package tn.gov.ancs.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Organisme;

import java.util.UUID;

@Repository
public interface OrganismeRepository extends JpaRepository<Organisme, UUID> {

    Page<Organisme> findBySecteurActivite(String secteurActivite, Pageable pageable);

    boolean existsByNomIgnoreCase(String nom);
}
