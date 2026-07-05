package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.gov.ancs.audit.domain.Constat;
import tn.gov.ancs.audit.domain.Controle;
import tn.gov.ancs.audit.domain.Mission;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;
import tn.gov.ancs.audit.dto.request.ConstatRequest;
import tn.gov.ancs.audit.dto.response.ConstatResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.ConstatRepository;
import tn.gov.ancs.audit.repository.ControleRepository;
import tn.gov.ancs.audit.repository.MissionRepository;
import tn.gov.ancs.audit.security.AuditAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConstatService {

    private final ConstatRepository constatRepository;
    private final MissionRepository missionRepository;
    private final ControleRepository controleRepository;
    private final StorageService storageService;

    @Transactional
    @AuditAction(action = "SUBMIT_CONSTAT", resource = "CONSTAT")
    public ConstatResponse submitConstat(ConstatRequest request, String auditorEmail) {
        Mission mission = missionRepository.findById(request.getMissionId())
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée avec l'id: " + request.getMissionId()));

        // Sécurité : seul l'auditeur assigné peut saisir des constats
        if (!mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(auditorEmail)) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
        }

        Controle controle = controleRepository.findById(request.getControleId())
            .orElseThrow(() -> new ResourceNotFoundException("Contrôle non trouvé avec l'id: " + request.getControleId()));

        // Vérifier si un constat existe déjà pour ce couple mission/contrôle
        Constat constat = constatRepository.findByMissionIdAndControleId(mission.getId(), controle.getId())
            .orElse(Constat.builder().mission(mission).controle(controle).build());

        constat.setResultat(request.getResultat());
        constat.setCommentaire(request.getCommentaire());
        constat.setDateConstat(Instant.now());
        constat.setSynced(true); // Directement synchronisé via l'API REST

        if (request.getPreuveUrl() != null) {
            constat.setPreuveUrl(request.getPreuveUrl());
        }

        Constat saved = constatRepository.save(constat);
        log.debug("Constat enregistré : mission {}, contrôle {}, résultat {}", 
            mission.getId(), controle.getId(), request.getResultat());

        return mapToResponse(saved);
    }

    @Transactional
    @AuditAction(action = "UPLOAD_PREUVE", resource = "CONSTAT", extractResourceId = true)
    public ConstatResponse uploadPreuve(UUID constatId, MultipartFile file, String auditorEmail) {
        Constat constat = constatRepository.findById(constatId)
            .orElseThrow(() -> new ResourceNotFoundException("Constat non trouvé avec l'id: " + constatId));

        // Sécurité : seul l'auditeur assigné à la mission concernée peut téléverser des preuves
        if (!constat.getMission().getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(auditorEmail)) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
        }

        // Envoi sur MinIO
        String objectName = storageService.uploadPreuve(file);
        
        constat.setPreuveUrl(objectName);
        Constat saved = constatRepository.save(constat);
        log.info("Preuve téléversée pour le constat {} : {}", constatId, objectName);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConstatResponse> getConstatsByMissionId(UUID missionId, String userEmail, String userRole, UUID userOrganismeId) {
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée"));

        // Vérification de sécurité d'accès aux constats
        if ("RSSI".equalsIgnoreCase(userRole) && !mission.getOrganisme().getId().equals(userOrganismeId)) {
            throw new AccessDeniedException("Accès refusé aux constats de cet organisme");
        }

        return constatRepository.findByMissionId(missionId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public ConstatResponse mapToResponse(Constat c) {
        // Obtenir l'URL pré-signée pour le frontend si une preuve existe
        String signedPreuveUrl = null;
        if (c.getPreuveUrl() != null && !c.getPreuveUrl().isEmpty()) {
            try {
                signedPreuveUrl = storageService.getPresignedUrl(storageService.getPreuvesBucket(), c.getPreuveUrl());
            } catch (Exception e) {
                log.warn("Impossible de générer l'URL sécurisée pour la preuve : {}", c.getPreuveUrl());
            }
        }

        return ConstatResponse.builder()
            .id(c.getId())
            .missionId(c.getMission().getId())
            .controleId(c.getControle().getId())
            .controleLibelle(c.getControle().getLibelle())
            .controleCategorie(c.getControle().getCategorie())
            .resultat(c.getResultat() != null ? c.getResultat().name() : null)
            .preuveUrl(signedPreuveUrl != null ? signedPreuveUrl : c.getPreuveUrl()) // pré-signée si possible
            .commentaire(c.getCommentaire())
            .dateConstat(c.getDateConstat())
            .synced(c.getSynced())
            .build();
    }
}
