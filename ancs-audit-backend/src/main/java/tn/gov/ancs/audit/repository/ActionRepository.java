package tn.gov.ancs.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.gov.ancs.audit.domain.Action;
import tn.gov.ancs.audit.domain.enums.PrioriteAction;
import tn.gov.ancs.audit.domain.enums.StatutAction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActionRepository extends JpaRepository<Action, UUID> {

    List<Action> findByConstatId(UUID constatId);

    @Query("""
        SELECT a FROM Action a
        JOIN a.constat c
        WHERE c.mission.id = :missionId
        ORDER BY a.priorite DESC, a.echeance ASC
        """)
    Page<Action> findByMissionId(UUID missionId, Pageable pageable);

    @Query("""
        SELECT a FROM Action a
        JOIN a.constat c
        WHERE c.mission.id = :missionId
        ORDER BY a.priorite DESC, a.echeance ASC
        """)
    List<Action> findActionsByMissionId(@org.springframework.data.repository.query.Param("missionId") UUID missionId);

    /**
     * Actions pour le dashboard RSSI — filtrées par organisme via la chaîne
     * Action → Constat → Mission → Organisme.
     */
    @Query("""
        SELECT a FROM Action a
        JOIN a.constat c
        JOIN c.mission m
        WHERE m.organisme.id = :organismeId AND a.statut != 'CLOTUREE'
        ORDER BY a.priorite DESC, a.echeance ASC
        """)
    List<Action> findActifsByOrganismeId(UUID organismeId);

    /** Actions en retard — utilisées par le scheduler de notifications FCM. */
    @Query("SELECT a FROM Action a WHERE a.statut IN ('A_FAIRE', 'EN_COURS') AND a.echeance < :aujourd_hui")
    List<Action> findEnRetard(LocalDate aujourd_hui);

    /** Actions dont l'échéance approche (rappel J-X). */
    @Query("SELECT a FROM Action a WHERE a.statut IN ('A_FAIRE', 'EN_COURS') AND a.echeance BETWEEN :debut AND :fin")
    List<Action> findEcheancesProches(LocalDate debut, LocalDate fin);

    /** Mise à jour en masse du statut EN_RETARD pour les actions échues. */
    @Modifying
    @Query("UPDATE Action a SET a.statut = 'EN_RETARD' WHERE a.statut IN ('A_FAIRE', 'EN_COURS') AND a.echeance < :aujourd_hui")
    int updateStatutEnRetard(LocalDate aujourd_hui);

    @Query("""
        SELECT COUNT(a) FROM Action a
        JOIN a.constat c
        JOIN c.mission m
        WHERE m.organisme.id = :organismeId AND a.statut = 'CLOTUREE'
        """)
    long countClotureesParOrganisme(@org.springframework.data.repository.query.Param("organismeId") UUID organismeId);

    long countByStatut(StatutAction statut);
}
