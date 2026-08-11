package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Auditeur;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutAuditeur;
import tn.gov.ancs.audit.dto.request.CreateAuditeurRequest;
import tn.gov.ancs.audit.dto.response.AuditeurResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.AuditeurRepository;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.security.AuditAction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditeurService {

    private final AuditeurRepository auditeurRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    @AuditAction(action = "CERTIFY_AUDITEUR", resource = "AUDITEUR")
    public AuditeurResponse certifyAuditeur(CreateAuditeurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + request.getUtilisateurId()));

        if (utilisateur.getRole() != Role.AUDITEUR) {
            throw new IllegalArgumentException("L'utilisateur associé doit posséder le rôle AUDITEUR");
        }

        if (auditeurRepository.existsByNumeroCertification(request.getNumeroCertification())) {
            throw new IllegalArgumentException("Ce numéro de certification est déjà enregistré");
        }

        if (request.getDateExpiration() != null && request.getDateCertification() != null && 
            !request.getDateExpiration().isAfter(request.getDateCertification())) {
            throw new IllegalArgumentException("La date d'expiration doit être ultérieure à la date de certification");
        }

        Auditeur auditeur = Auditeur.builder()
            .utilisateur(utilisateur)
            .numeroCertification(request.getNumeroCertification())
            .dateCertification(request.getDateCertification())
            .dateExpiration(request.getDateExpiration())
            .specialites(request.getSpecialites())
            .statut(StatutAuditeur.ACTIF)
            .build();

        Auditeur saved = auditeurRepository.save(auditeur);
        log.info("Nouvel auditeur certifié enregistré : {} (Num: {})", utilisateur.getEmail(), saved.getNumeroCertification());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuditeurResponse getAuditeurById(UUID id) {
        Auditeur auditeur = auditeurRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Auditeur non trouvé avec l'id: " + id));
        return mapToResponse(auditeur);
    }

    @Transactional(readOnly = true)
    public Page<AuditeurResponse> getAuditeurs(StatutAuditeur statut, Pageable pageable) {
        Page<Auditeur> auditeurs = (statut != null) 
            ? auditeurRepository.findByStatut(statut, pageable)
            : auditeurRepository.findAll(pageable);
        return auditeurs.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditeurResponse> getAllAuditeursList() {
        return auditeurRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Tâche planifiée pour mettre à jour automatiquement les certifications expirées.
     */
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void verifyCertificationsExpiration() {
        LocalDate today = LocalDate.now();
        List<Auditeur> expirant = auditeurRepository.findActifsExpirantAvant(today);
        
        for (Auditeur auditeur : expirant) {
            auditeur.setStatut(StatutAuditeur.EXPIRE);
            auditeurRepository.save(auditeur);
            log.info("La certification de l'auditeur {} (Num: {}) a expiré aujourd'hui", 
                auditeur.getUtilisateur().getEmail(), auditeur.getNumeroCertification());
        }
    }

    private AuditeurResponse mapToResponse(Auditeur auditeur) {
        return AuditeurResponse.builder()
            .id(auditeur.getId())
            .utilisateurId(auditeur.getUtilisateur().getId())
            .nom(auditeur.getUtilisateur().getNom())
            .email(auditeur.getUtilisateur().getEmail())
            .numeroCertification(auditeur.getNumeroCertification())
            .dateCertification(auditeur.getDateCertification())
            .dateExpiration(auditeur.getDateExpiration())
            .specialites(auditeur.getSpecialites())
            .statut(auditeur.getStatut().name())
            .build();
    }
}
