package tn.gov.ancs.audit.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.*;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.dto.request.AiSummaryRequest;
import tn.gov.ancs.audit.dto.response.SyntheseIaResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.*;
import tn.gov.ancs.audit.security.AuditAction;
import tn.gov.ancs.audit.service.ai.AiSummaryService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RapportService {

    private final RapportRepository rapportRepository;
    private final MissionRepository missionRepository;
    private final ConstatRepository constatRepository;
    private final ActionRepository actionRepository;
    private final StorageService storageService;
    private final AiSummaryService aiSummaryService;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Appelle le service LLM local (Ollama) pour obtenir le brouillon de synthèse.
     */
    @Transactional
    @AuditAction(action = "GENERATE_SYNTHESE_IA", resource = "MISSION", extractResourceId = true)
    public SyntheseIaResponse generateSyntheseIa(UUID missionId, String langue, String auditorEmail) {
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée"));

        // Seul l'auditeur assigné ou l'admin peut appeler l'IA
        Utilisateur caller = utilisateurRepository.findByEmailIgnoreCase(auditorEmail)
            .orElseThrow(() -> new AccessDeniedException("Accès refusé"));

        boolean isAssignedAuditor = mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(auditorEmail);
        boolean isAdmin = caller.getRole() == Role.ADMIN_ANCS;

        if (!isAssignedAuditor && !isAdmin) {
            throw new AccessDeniedException("Accès refusé");
        }

        // Calculer le taux de conformité
        Double compliance = constatRepository.calculateTauxConformite(missionId);
        if (compliance == null) compliance = 0.0;

        // Récupérer les constats pour alimenter le prompt
        List<Constat> constats = constatRepository.findByMissionId(missionId);
        List<AiSummaryRequest.ConstatInfo> constatInfos = constats.stream()
            .map(c -> AiSummaryRequest.ConstatInfo.builder()
                .controleLibelle(c.getControle().getLibelle())
                .resultat(c.getResultat() != null ? c.getResultat().name() : "NON_SAISI")
                .criticite(c.getControle().getCriticite())
                .commentaire(c.getCommentaire())
                .build()
            ).collect(Collectors.toList());

        AiSummaryRequest aiRequest = AiSummaryRequest.builder()
            .organismeNom(mission.getOrganisme().getNom())
            .perimetre(mission.getPerimetre())
            .dateDebut(mission.getDateDebut() != null ? mission.getDateDebut().toString() : null)
            .dateFin(mission.getDateFin() != null ? mission.getDateFin().toString() : null)
            .referentielNom(mission.getReferentiel().getNom())
            .referentielVersion(mission.getReferentiel().getVersion())
            .tauxConformite(compliance)
            .langue(langue != null ? langue : "FR")
            .constats(constatInfos)
            .build();

        String draft = aiSummaryService.generateDraft(aiRequest);

        // Avertissement de souveraineté si OpenAI est sélectionné (pour traçabilité dans la réponse)
        String warning = null;
        if (aiSummaryService.getClass().getSimpleName().startsWith("OpenAi")) {
            warning = "DATA_SENT_TO_THIRD_PARTY";
        }

        return SyntheseIaResponse.builder()
            .brouillon(draft)
            .avertissement(warning)
            .build();
    }

    /**
     * Génère et téléverse sur MinIO le rapport officiel de la mission (PDF ou Word).
     */
    @Transactional
    @AuditAction(action = "GENERATE_RAPPORT", resource = "RAPPORT")
    public Rapport generateRapport(UUID missionId, String type, String syntheseExecutive, boolean isIaGenerated, String callerEmail) {
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée"));

        // SÉCURITÉ : Contrôle d'accès propriétaire (IDOR) et rôle Admin
        Utilisateur caller = utilisateurRepository.findByEmailIgnoreCase(callerEmail)
            .orElseThrow(() -> new AccessDeniedException("Accès refusé"));

        boolean isAssignedAuditor = mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(callerEmail);
        boolean isAdmin = caller.getRole() == Role.ADMIN_ANCS;

        if (!isAssignedAuditor && !isAdmin) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
        }

        List<Constat> constats = constatRepository.findByMissionId(missionId);
        Double compliance = constatRepository.calculateTauxConformite(missionId);
        if (compliance == null) compliance = 0.0;

        String filename = "Rapport_Audit_" + mission.getOrganisme().getNom().replace(" ", "_") + "." + type.toLowerCase();
        String objectName = null;

        if ("PDF".equalsIgnoreCase(type)) {
            byte[] pdfBytes = buildPdfRapport(mission, constats, compliance, syntheseExecutive);
            objectName = storageService.uploadRapport(filename, new ByteArrayInputStream(pdfBytes), "application/pdf");
        } else if ("DOCX".equalsIgnoreCase(type)) {
            byte[] docxBytes = buildDocxRapport(mission, constats, compliance, syntheseExecutive);
            objectName = storageService.uploadRapport(filename, new ByteArrayInputStream(docxBytes), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else {
            throw new IllegalArgumentException("Format de rapport non pris en charge: " + type);
        }

        // Créer l'entité Rapport
        int nextVersion = rapportRepository.getNextVersionForMission(missionId);

        Rapport rapport = Rapport.builder()
            .mission(mission)
            .fichierUrl(objectName)
            .dateGeneration(Instant.now())
            .version(nextVersion)
            .type(type.toUpperCase())
            .syntheseGenereeParIa(isIaGenerated)
            .syntheseIaHorodatage(isIaGenerated ? Instant.now() : null)
            .build();

        return rapportRepository.save(rapport);
    }

    /**
     * Récupère le lien de téléchargement sécurisé pré-signé pour un rapport d'audit.
     */
    @Transactional(readOnly = true)
    @AuditAction(action = "DOWNLOAD_RAPPORT", resource = "RAPPORT", extractResourceId = true)
    public String getRapportDownloadUrl(UUID rapportId, String userEmail, Role userRole, UUID userOrganismeId) {
        Rapport rapport = rapportRepository.findById(rapportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        // Contrôle d'accès : le RSSI ne peut télécharger que les rapports de son organisme
        if (userRole == Role.RSSI) {
            UUID targetOrgId = rapport.getMission().getOrganisme().getId();
            if (!targetOrgId.equals(userOrganismeId)) {
                throw new AccessDeniedException("Accès refusé : vous n'avez pas l'autorisation d'accéder au rapport d'un autre organisme");
            }
        }

        return storageService.getPresignedUrl(storageService.getRapportsBucket(), rapport.getFichierUrl());
    }

    // ========================================================
    // Moteurs de rendu iText PDF & Apache POI DOCX
    // ========================================================

    private byte[] buildPdfRapport(Mission m, List<Constat> constats, double compliance, String synthese) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // En-tête institutionnel
            doc.add(new Paragraph("RÉPUBLIQUE TUNISIENNE").setTextAlignment(TextAlignment.CENTER).setBold());
            doc.add(new Paragraph("AGENCE NATIONALE DE CYBERSÉCURITÉ (ANCS)").setTextAlignment(TextAlignment.CENTER).setBold());
            doc.add(new Paragraph("---------------------------------------------------------------------------------").setTextAlignment(TextAlignment.CENTER));
            
            doc.add(new Paragraph("\nRAPPORT OFFICIEL D'AUDIT DE SÉCURITÉ SI").setTextAlignment(TextAlignment.CENTER).setFontSize(18).setBold());
            doc.add(new Paragraph("Organisme audité : " + m.getOrganisme().getNom()).setFontSize(14).setBold());
            doc.add(new Paragraph("Périmètre de l'audit : " + (m.getPerimetre() != null ? m.getPerimetre() : "Non spécifié")));
            doc.add(new Paragraph("Référentiel appliqué : " + m.getReferentiel().getNom()));
            doc.add(new Paragraph("Taux global de conformité : " + String.format("%.1f", compliance) + "%").setBold());
            doc.add(new Paragraph("\n"));

            // Section Synthèse Exécutive
            doc.add(new Paragraph("1. SYNTHÈSE EXÉCUTIVE").setFontSize(14).setBold());
            doc.add(new Paragraph(synthese != null ? synthese : "Aucune synthèse rédigée."));
            doc.add(new Paragraph("\n"));

            // Section Résultats détaillés
            doc.add(new Paragraph("2. RÉSULTATS DÉTAILLÉS PAR CONTRÔLE").setFontSize(14).setBold());
            
            Table table = new Table(3);
            table.addHeaderCell("Contrôle technique");
            table.addHeaderCell("Statut");
            table.addHeaderCell("Commentaire de l'auditeur");

            for (Constat c : constats) {
                table.addCell(c.getControle().getLibelle());
                table.addCell(c.getResultat() != null ? c.getResultat().name() : "NON_EVALUE");
                table.addCell(c.getCommentaire() != null ? c.getCommentaire() : "");
            }
            doc.add(table);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Échec de la génération du rapport PDF", e);
            throw new RuntimeException("Erreur de génération PDF", e);
        }
    }

    private byte[] buildDocxRapport(Mission m, List<Constat> constats, double compliance, String synthese) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Titres et En-tête
            XWPFParagraph p1 = doc.createParagraph();
            p1.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            XWPFRun r1 = p1.createRun();
            r1.setText("RÉPUBLIQUE TUNISIENNE");
            r1.setBold(true);
            
            XWPFParagraph p2 = doc.createParagraph();
            p2.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            XWPFRun r2 = p2.createRun();
            r2.setText("AGENCE NATIONALE DE CYBERSÉCURITÉ (ANCS)");
            r2.setBold(true);

            XWPFParagraph pTitle = doc.createParagraph();
            XWPFRun rTitle = pTitle.createRun();
            rTitle.setText("\nRAPPORT D'AUDIT DE SÉCURITÉ SI — " + m.getOrganisme().getNom());
            rTitle.setFontSize(16);
            rTitle.setBold(true);

            // Métadonnées
            XWPFParagraph pMeta = doc.createParagraph();
            XWPFRun rMeta = pMeta.createRun();
            rMeta.setText("Taux de conformité global: " + String.format("%.1f", compliance) + "%");
            rMeta.setBold(true);

            // Synthèse
            XWPFParagraph pSection1 = doc.createParagraph();
            XWPFRun rSection1 = pSection1.createRun();
            rSection1.setText("\n1. SYNTHÈSE EXÉCUTIVE\n");
            rSection1.setBold(true);

            XWPFParagraph pSynth = doc.createParagraph();
            XWPFRun rSynth = pSynth.createRun();
            rSynth.setText(synthese != null ? synthese : "Aucune synthèse rédigée.");

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Échec de la génération du rapport Word (DOCX)", e);
            throw new RuntimeException("Erreur de génération DOCX", e);
        }
    }
}
