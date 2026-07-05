package tn.gov.ancs.audit.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Auditeur;
import tn.gov.ancs.audit.domain.Organisme;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutAuditeur;
import tn.gov.ancs.audit.repository.AuditeurRepository;
import tn.gov.ancs.audit.repository.OrganismeRepository;
import tn.gov.ancs.audit.repository.UtilisateurRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Initialisation des données de démonstration pour l'environnement de développement ou de test.
 *
 * <p>Sécurité : Cette classe s'exécute uniquement si le profil 'dev' ou 'test' est activé.
 * Elle garantit qu'aucune donnée de test ou compte de démonstration avec mot de passe
 * par défaut ne soit déployé dans l'environnement de production.</p>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevDataLoader implements ApplicationRunner {

    private final OrganismeRepository organismeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditeurRepository auditeurRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Démarrage du chargeur de données de démonstration (Profils de développement/test actifs).");

        // 1. Création des organismes de démonstration
        Organisme bnt = getOrCreateOrganisme(
            "Banque Nationale de Tunisie (BNT) — DEMO",
            "Banques et établissements financiers",
            "SOUMIS_AUDIT",
            "15 Avenue de France, 1000 Tunis, Tunisie",
            "rssi.demo@bnt.com.tn"
        );

        getOrCreateOrganisme(
            "Tunisie Télécom — DEMO",
            "Télécommunications",
            "SOUMIS_AUDIT",
            "Rue de l'Arabie Saoudite, 1080 Tunis, Tunisie",
            "rssi.demo@tunisietelecom.tn"
        );

        getOrCreateOrganisme(
            "Ministère des Finances — DEMO",
            "Secteur public — Administration centrale",
            "SOUMIS_AUDIT",
            "Place du Gouvernement, Kasbah, 1019 Tunis, Tunisie",
            "rssi.demo@finances.gov.tn"
        );

        // 2. Création de l'auditeur de démonstration
        Utilisateur userAuditeur = getOrCreateUtilisateur(
            "Mohamed Ben Ali",
            "auditeur.demo@ancs.gov.tn",
            // Hash BCrypt(12) de "Auditeur@ANCS2024!"
            "$2a$12$QoW6t8pU7SiXuGqP4nHHiOF/3PZi0/pDW7z5lR2vZ2X1K9mXQlJGG",
            Role.AUDITEUR,
            null
        );

        getOrCreateAuditeur(
            userAuditeur,
            "ANCS-AUD-2024-001",
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2027, 1, 15),
            List.of("ISO/IEC 27001", "EBIOS RM", "Sécurité réseau", "Cloud Security"),
            StatutAuditeur.ACTIF
        );

        // 3. Création du RSSI de démonstration lié au premier organisme (BNT)
        getOrCreateUtilisateur(
            "Fatima Zahra Trabelsi",
            "rssi.demo@bnt.com.tn",
            // Hash BCrypt(12) de "Rssi@ANCS2024!"
            "$2a$12$NxZmVoY8pJ3qKlR5tW2HhOKdF6aBXTzP0mWk8sLnQ4uY2vC1jRqI.",
            Role.RSSI,
            bnt
        );

        log.info("Chargement des données de démonstration terminé.");
    }

    private Organisme getOrCreateOrganisme(String nom, String secteur, String obligation, String adresse, String contactEmail) {
        Optional<Organisme> existing = organismeRepository.findAll().stream()
            .filter(o -> o.getNom().equalsIgnoreCase(nom))
            .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        Organisme org = Organisme.builder()
            .nom(nom)
            .secteurActivite(secteur)
            .typeObligationAudit(obligation)
            .adresse(adresse)
            .contactRssiEmail(contactEmail)
            .build();

        return organismeRepository.save(org);
    }

    private Utilisateur getOrCreateUtilisateur(String nom, String email, String passwordHash, Role role, Organisme organisme) {
        Optional<Utilisateur> existing = utilisateurRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        Utilisateur user = Utilisateur.builder()
            .nom(nom)
            .email(email)
            .passwordHash(passwordHash)
            .role(role)
            .organisme(organisme)
            .isActive(true)
            .totpEnabled(false)
            .build();

        return utilisateurRepository.save(user);
    }

    private void getOrCreateAuditeur(Utilisateur user, String numCertif, LocalDate dateCertif, LocalDate dateExp, List<String> specialites, StatutAuditeur statut) {
        Optional<Auditeur> existing = auditeurRepository.findAll().stream()
            .filter(a -> a.getNumeroCertification().equalsIgnoreCase(numCertif))
            .findFirst();

        if (existing.isPresent()) {
            return;
        }

        Auditeur auditeur = Auditeur.builder()
            .utilisateur(user)
            .numeroCertification(numCertif)
            .dateCertification(dateCertif)
            .dateExpiration(dateExp)
            .specialites(specialites)
            .statut(statut)
            .build();

        auditeurRepository.save(auditeur);
    }
}
