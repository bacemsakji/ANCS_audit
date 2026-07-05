package tn.gov.ancs.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Controle;

import java.util.List;
import java.util.UUID;

@Repository
public interface ControleRepository extends JpaRepository<Controle, UUID> {

    /** Retourne tous les contrôles d'un référentiel, triés pour la checklist. */
    List<Controle> findByReferentielIdOrderByCategorieAscOrdreAffichageAsc(UUID referentielId);

    List<Controle> findByReferentielIdAndCategorie(UUID referentielId, String categorie);

    @Query("SELECT DISTINCT c.categorie FROM Controle c WHERE c.referentiel.id = :referentielId ORDER BY c.categorie")
    List<String> findCategoriesByReferentielId(UUID referentielId);

    long countByReferentielId(UUID referentielId);
}
