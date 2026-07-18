package tn.gov.ancs.audit.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.colors.ColorConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.*;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutSoumissionAncs;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
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

        // Récupérer le prochain numéro de version
        int nextVersion = rapportRepository.getNextVersionForMission(missionId);

        // Récupérer le nom de l'auditeur et ses infos
        String nomAuditeur = mission.getAuditeur().getUtilisateur().getNom();
        String numCertif = mission.getAuditeur().getNumeroCertification();
        String contactAuditeur = mission.getAuditeur().getUtilisateur().getEmail();
        String confidentialite = "Le présent document est confidentiel et sa confidentialité consiste à :\n" +
            "- La non divulgation desdites informations confidentielles auprès de tierce partie,\n" +
            "- La non reproduction des informations dites confidentielles, sauf accord de l’organisme audité,\n" +
            "- Ne pas profiter ou faire profiter tierce partie du contenu de ces informations en matière de savoir-faire,\n" +
            "- Considérer toutes les informations relatives à la production et au système d’information de l’organisme audité déclarées Confidentielles.";
        
        java.util.Optional<Rapport> previousRapportOpt = rapportRepository.findFirstByMissionIdOrderByVersionDesc(missionId);
        String currentHistEntry = "Version " + nextVersion + " - " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now()) + " - " + nomAuditeur + " - Génération du rapport";
        String historique = previousRapportOpt.isPresent() && previousRapportOpt.get().getHistoriqueVersions() != null
            ? previousRapportOpt.get().getHistoriqueVersions() + "\n" + currentHistEntry
            : currentHistEntry;

        if ("PDF".equalsIgnoreCase(type)) {
            byte[] pdfBytes = buildPdfRapport(mission, constats, compliance, syntheseExecutive, nextVersion, nomAuditeur, numCertif, contactAuditeur, confidentialite, historique);
            objectName = storageService.uploadRapport(filename, new ByteArrayInputStream(pdfBytes), "application/pdf");
        } else if ("DOCX".equalsIgnoreCase(type)) {
            byte[] docxBytes = buildDocxRapport(mission, constats, compliance, syntheseExecutive, nextVersion, nomAuditeur, numCertif, contactAuditeur, confidentialite, historique);
            objectName = storageService.uploadRapport(filename, new ByteArrayInputStream(docxBytes), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else {
            throw new IllegalArgumentException("Format de rapport non pris en charge: " + type);
        }

        // Créer l'entité Rapport
        Rapport rapport = Rapport.builder()
            .mission(mission)
            .fichierUrl(objectName)
            .dateGeneration(Instant.now())
            .version(nextVersion)
            .type(type.toUpperCase())
            .syntheseGenereeParIa(isIaGenerated)
            .syntheseIaHorodatage(isIaGenerated ? Instant.now() : null)
            // ANCS v2.1 fields
            .nomAuditeur(nomAuditeur)
            .numeroCertificationAncs(numCertif)
            .contactAuditeur(contactAuditeur)
            .texteConfidentialite(confidentialite)
            .historiqueVersions(historique)
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

        // Contrôle d'accès : un AUDITEUR ne peut télécharger que les rapports
        // des missions qui lui sont assignées — empêche le téléchargement
        // par ID (IDOR) vers une mission d'un autre auditeur.
        if (userRole == Role.AUDITEUR) {
            String assignedEmail = rapport.getMission().getAuditeur().getUtilisateur().getEmail();
            if (!assignedEmail.equalsIgnoreCase(userEmail)) {
                throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
            }
        }

        return storageService.getPresignedUrl(storageService.getRapportsBucket(), rapport.getFichierUrl());
    }

    // ========================================================
    // Moteurs de rendu iText PDF & Apache POI DOCX
    // ========================================================

    private byte[] buildPdfRapport(Mission m, List<Constat> constats, double compliance, String synthese,
                                   int version, String nomAuditeur, String numCertif, String contactAuditeur,
                                   String confidentialite, String historique) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // --- PAGE DE GARDE ---
            doc.add(new Paragraph("RÉPUBLIQUE TUNISIENNE").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(11));
            doc.add(new Paragraph("AGENCE NATIONALE DE CYBERSÉCURITÉ (ANCS)").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(11));
            doc.add(new Paragraph("---------------------------------------------------------------------------------").setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("\n\n\n"));
            doc.add(new Paragraph("RAPPORT D’AUDIT DE LA SÉCURITÉ DU SYSTÈME D’INFORMATION").setTextAlignment(TextAlignment.CENTER).setFontSize(18).setBold());
            doc.add(new Paragraph("De : " + m.getOrganisme().getNom()).setTextAlignment(TextAlignment.CENTER).setFontSize(14).setBold());
            doc.add(new Paragraph("\n\n\n"));

            // Infos Expert Auditeur et signatures
            Table coverTable = new Table(3);
            coverTable.addCell(new Cell().add(new Paragraph("Expert Auditeur chargé de la mission :\n" + nomAuditeur + "\nN° Certif: " + numCertif + "\nContact: " + contactAuditeur).setFontSize(9)));
            coverTable.addCell(new Cell().add(new Paragraph("Cachet de l'auditeur :\n\n\n\n").setFontSize(9)));
            coverTable.addCell(new Cell().add(new Paragraph("Signature :\n\n\n\n").setFontSize(9)));
            doc.add(coverTable);

            doc.add(new Paragraph("\n\n"));
            doc.add(new Paragraph("Version : " + version).setFontSize(9));
            doc.add(new Paragraph("Date : " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now())).setFontSize(9));
            doc.add(new Paragraph("Diffusion : Document Confidentiel").setFontSize(9).setBold());

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 1. AVANT-PROPOS ---
            doc.add(new Paragraph("1. Avant-propos").setFontSize(14).setBold());
            doc.add(new Paragraph("1.1 Confidentialité du document").setFontSize(11).setBold());
            doc.add(new Paragraph(confidentialite).setFontSize(9));
            
            doc.add(new Paragraph("\n1.2 Historique des modifications").setFontSize(11).setBold());
            Table histTable = new Table(4);
            histTable.addHeaderCell(new Cell().add(new Paragraph("Version").setBold().setFontSize(9)));
            histTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold().setFontSize(9)));
            histTable.addHeaderCell(new Cell().add(new Paragraph("Auteur").setBold().setFontSize(9)));
            histTable.addHeaderCell(new Cell().add(new Paragraph("Modifications").setBold().setFontSize(9)));
            
            if (historique != null && !historique.trim().isEmpty()) {
                String[] histLines = historique.split("\n");
                for (String line : histLines) {
                    String[] parts = line.split(" - ");
                    if (parts.length >= 4) {
                        histTable.addCell(new Cell().add(new Paragraph(parts[0]).setFontSize(9)));
                        histTable.addCell(new Cell().add(new Paragraph(parts[1]).setFontSize(9)));
                        histTable.addCell(new Cell().add(new Paragraph(parts[2]).setFontSize(9)));
                        histTable.addCell(new Cell().add(new Paragraph(parts[3]).setFontSize(9)));
                    } else {
                        histTable.addCell(new Cell().add(new Paragraph(line).setFontSize(9)));
                        histTable.addCell(new Cell().add(new Paragraph("-").setFontSize(9)));
                        histTable.addCell(new Cell().add(new Paragraph("-").setFontSize(9)));
                        histTable.addCell(new Cell().add(new Paragraph("-").setFontSize(9)));
                    }
                }
            }
            doc.add(histTable);

            doc.add(new Paragraph("\n1.3 Diffusion du document").setFontSize(11).setBold());
            doc.add(new Paragraph("Diffusion côté Expert Auditeur :").setFontSize(9).setBold());
            Table diffAuditeur = new Table(3);
            diffAuditeur.addHeaderCell(new Cell().add(new Paragraph("Nom & Prénom").setBold().setFontSize(9)));
            diffAuditeur.addHeaderCell(new Cell().add(new Paragraph("Titre").setBold().setFontSize(9)));
            diffAuditeur.addHeaderCell(new Cell().add(new Paragraph("Contact").setBold().setFontSize(9)));
            diffAuditeur.addCell(new Cell().add(new Paragraph(nomAuditeur).setFontSize(9)));
            diffAuditeur.addCell(new Cell().add(new Paragraph("Expert Auditeur Cybersécurité").setFontSize(9)));
            diffAuditeur.addCell(new Cell().add(new Paragraph(contactAuditeur).setFontSize(9)));
            doc.add(diffAuditeur);

            doc.add(new Paragraph("\nDiffusion côté Organisme Audité :").setFontSize(9).setBold());
            Table diffOrganisme = new Table(3);
            diffOrganisme.addHeaderCell(new Cell().add(new Paragraph("Nom de l'organisme").setBold().setFontSize(9)));
            diffOrganisme.addHeaderCell(new Cell().add(new Paragraph("Contact RSSI").setBold().setFontSize(9)));
            diffOrganisme.addHeaderCell(new Cell().add(new Paragraph("Rôle").setBold().setFontSize(9)));
            diffOrganisme.addCell(new Cell().add(new Paragraph(m.getOrganisme().getNom()).setFontSize(9)));
            diffOrganisme.addCell(new Cell().add(new Paragraph(m.getOrganisme().getContactRssiEmail() != null ? m.getOrganisme().getContactRssiEmail() : "-").setFontSize(9)));
            diffOrganisme.addCell(new Cell().add(new Paragraph("Responsable de la Sécurité des SI").setFontSize(9)));
            doc.add(diffOrganisme);

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 2. CADRE DE LA MISSION ---
            doc.add(new Paragraph("2. Cadre de la mission").setFontSize(14).setBold());
            doc.add(new Paragraph("La présente mission d'audit est réalisée en application directe du décret-loi n° 2023-17 du 11 mars 2023 relatif à la cybersécurité, et de l'arrêté du ministre des technologies de la communication du 12 Septembre 2023, fixant les critères techniques d'audit et les modalités de suivi de la mise en œuvre des recommandations contenues dans le rapport d'audit.\n\n" +
                "Il s'agit d'un audit de sécurité réglementaire périodique visant à mesurer l'état de conformité du système d'information de l'organisme par rapport au référentiel ANCS et de proposer un plan d'action de remédiation adéquat.").setFontSize(10));

            // --- 3. TERMES ET DÉFINITIONS ---
            doc.add(new Paragraph("\n3. Termes et définitions").setFontSize(14).setBold());
            doc.add(new Paragraph("- Cybersécurité : État de protection du cyberespace assurant la confidentialité, l'intégrité et la disponibilité des données.\n" +
                "- Audit de sécurité : Processus systématique, indépendant et documenté permettant d'évaluer l'état de la sécurité par rapport à des critères d'audit.\n" +
                "- Constat : Résultat de l'évaluation des preuves d'audit par rapport aux critères d'audit.\n" +
                "- Plan d'action : Ensemble de mesures correctives ordonnées et planifiées visant à traiter les non-conformités.").setFontSize(10));

            // --- 4. RÉFÉRENCES ---
            doc.add(new Paragraph("\n4. Références").setFontSize(14).setBold());
            doc.add(new Paragraph("- Décret-loi n° 2023-17 du 11 mars 2023 relatif à la cybersécurité.\n" +
                "- Arrêté du 12 Septembre 2023 fixant les critères techniques d'audit ANCS.\n" +
                "- Norme de référence : " + m.getReferentiel().getNom() + " (Version: " + m.getReferentiel().getVersion() + ").").setFontSize(10));

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 5. PRÉSENTATION DE L'ORGANISME ---
            doc.add(new Paragraph("5. Présentation de l'organisme").setFontSize(14).setBold());
            Table orgTable = new Table(2);
            orgTable.addCell(new Cell().add(new Paragraph("Nom de l'organisme").setBold().setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph(m.getOrganisme().getNom()).setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph("Secteur d'activité").setBold().setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph(m.getOrganisme().getSecteurActivite() != null ? m.getOrganisme().getSecteurActivite() : "Non spécifié").setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph("Type d'obligation").setBold().setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph(m.getOrganisme().getTypeObligationAudit() != null ? m.getOrganisme().getTypeObligationAudit() : "SOUMIS_AUDIT").setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph("Adresse du siège").setBold().setFontSize(9)));
            orgTable.addCell(new Cell().add(new Paragraph(m.getOrganisme().getAdresse() != null ? m.getOrganisme().getAdresse() : "Non spécifiée").setFontSize(9)));
            doc.add(orgTable);

            // --- 6. CHAMP D'AUDIT (PÉRIMÈTRE) ---
            doc.add(new Paragraph("\n6. Champ d'audit (Périmètre)").setFontSize(14).setBold());
            doc.add(new Paragraph("6.1 Périmètre géographique").setFontSize(11).setBold());
            doc.add(new Paragraph("Les vérifications physiques et logiques couvrent le siège social et l'infrastructure d'hébergement de l'organisme situé à : " + (m.getOrganisme().getAdresse() != null ? m.getOrganisme().getAdresse() : "Siège principal") + ".").setFontSize(10));
            doc.add(new Paragraph("\n6.2 Périmètre logique").setFontSize(11).setBold());
            doc.add(new Paragraph("Périmètre technique ciblé : " + (m.getPerimetre() != null ? m.getPerimetre() : "L'ensemble du système d'information de production de l'organisme.")).setFontSize(10));

            // --- 7. MÉTHODOLOGIE ---
            doc.add(new Paragraph("\n7. Méthodologie").setFontSize(14).setBold());
            doc.add(new Paragraph("L'audit a été mené selon la méthodologie standard de l'ANCS, comprenant :\n" +
                "- Des entretiens avec les équipes techniques et d'organisation (RSSI, administrateurs, etc.).\n" +
                "- Des revues documentaires (politiques de sécurité, procédures, configurations réseau).\n" +
                "- Des tests de conformité techniques et fonctionnels sur les postes de travail, serveurs et équipements de sécurité.\n\n" +
                "Équipe intervenante :\n" +
                "- " + nomAuditeur + " (Expert Auditeur certifié par l'ANCS — Numéro de certification: " + numCertif + ").").setFontSize(10));

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 8. SYNTHÈSE DES RÉSULTATS ---
            doc.add(new Paragraph("8. Synthèse des résultats").setFontSize(14).setBold());
            doc.add(new Paragraph("Taux global de conformité : " + String.format("%.1f", compliance) + "%").setBold().setFontSize(12));
            doc.add(new Paragraph("\nSynthèse exécutive :").setBold().setFontSize(11));
            doc.add(new Paragraph(synthese != null ? synthese : "Aucune synthèse rédigée.").setFontSize(10));

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 9. PRÉSENTATION DÉTAILLÉE DES RÉSULTATS DE L'AUDIT ---
            doc.add(new Paragraph("9. Présentation détaillée des résultats de l'audit").setFontSize(14).setBold());
            doc.add(new Paragraph("Les résultats de conformité sont présentés ci-dessous, groupés par domaine/critère du référentiel d'audit de sécurité des systèmes d'information conformément au paragraphe 9 du modèle ANCS.").setFontSize(10));

            // Grouper les constats par catégorie
            Map<String, List<Constat>> groupedConstats = new LinkedHashMap<>();
            for (Constat c : constats) {
                String cat = c.getControle().getCategorie();
                if (cat == null || cat.trim().isEmpty()) {
                    cat = "Mesures générales";
                }
                groupedConstats.computeIfAbsent(cat, k -> new ArrayList<>()).add(c);
            }

            for (Map.Entry<String, List<Constat>> entry : groupedConstats.entrySet()) {
                doc.add(new Paragraph("\nDomaine : " + entry.getKey()).setFontSize(12).setBold().setFontColor(ColorConstants.BLUE));
                
                for (Constat c : entry.getValue()) {
                    doc.add(new Paragraph("Critère : " + c.getControle().getLibelle()).setBold().setFontSize(10));
                    doc.add(new Paragraph("Résultat de l'évaluation : " + (c.getResultat() != null ? c.getResultat().name() : "NON_EVALUE")).setFontSize(9));
                    doc.add(new Paragraph("Description / Commentaire : " + (c.getCommentaire() != null ? c.getCommentaire() : "Aucun commentaire.")).setFontSize(9));
                    
                    if (c.getResultat() == ResultatConstat.NON_CONFORME || c.getResultat() == ResultatConstat.OBSERVATION) {
                        String crit = c.getCriticite() != null ? c.getCriticite() : (c.getControle().getCriticite() != null ? c.getControle().getCriticite() : "MOYEN");
                        doc.add(new Paragraph("  - Criticité : " + crit).setFontSize(9));
                        if (c.getPreuveDescription() != null && !c.getPreuveDescription().isEmpty()) {
                            doc.add(new Paragraph("  - Preuve d'audit : " + c.getPreuveDescription()).setFontSize(9));
                        }
                        if (c.getComposantesImpactees() != null && !c.getComposantesImpactees().isEmpty()) {
                            doc.add(new Paragraph("  - Composantes du SI impactées : " + c.getComposantesImpactees()).setFontSize(9));
                        }
                        if (c.getRecommandation() != null && !c.getRecommandation().isEmpty()) {
                            doc.add(new Paragraph("  - Recommandation : " + c.getRecommandation()).setFontSize(9).setItalic());
                        }
                    }
                    doc.add(new Paragraph("--------------------------------------------------").setFontSize(8).setFontColor(ColorConstants.GRAY));
                }
            }

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 10. APPRÉCIATION DES RISQUES ---
            doc.add(new Paragraph("10. Appréciation des risques").setFontSize(14).setBold());
            doc.add(new Paragraph("Conformément aux exigences de gestion des risques (ISO 27005 / EBIOS RM), les non-conformités identifiées engendrent les scénarios de risques prioritaires suivants :").setFontSize(10));
            
            int riskIndex = 1;
            for (Constat c : constats) {
                if (c.getResultat() == ResultatConstat.NON_CONFORME) {
                    String crit = c.getCriticite() != null ? c.getCriticite() : (c.getControle().getCriticite() != null ? c.getControle().getCriticite() : "MOYEN");
                    doc.add(new Paragraph(String.format("\nScénario du risque n°%d : Exploitation de la vulnérabilité liée à \"%s\"", riskIndex++, c.getControle().getLibelle())).setBold().setFontSize(10));
                    doc.add(new Paragraph("  - Description : Menace d'accès non autorisé ou de compromission par manque de contrôle conforme.").setFontSize(9));
                    doc.add(new Paragraph("  - Composante impactée : " + (c.getComposantesImpactees() != null ? c.getComposantesImpactees() : "Actifs SI liés")).setFontSize(9));
                    doc.add(new Paragraph("  - Gravité du risque : " + crit).setFontSize(9).setBold());
                    doc.add(new Paragraph("  - Recommandation associée : " + (c.getRecommandation() != null ? c.getRecommandation() : "Mettre en œuvre les correctifs")).setFontSize(9).setItalic());
                }
            }

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 11. MATURITÉ DES CONTRÔLES (ANNEXE 6) ---
            doc.add(new Paragraph("11. Tableau de maturité des contrôles (Annexe 6)").setFontSize(14).setBold());
            doc.add(new Paragraph("Évaluation de la maturité des contrôles de sécurité sur une échelle de 0 à 5 basée sur la conformité constatée (0: Inexistant, 1: Initial, 2: Répétable, 3: Défini, 4: Géré, 5: Optimisé).").setFontSize(10));
            
            Table matTable = new Table(5);
            matTable.addHeaderCell(new Cell().add(new Paragraph("Réf (Sous-critère)").setBold().setFontSize(9)));
            matTable.addHeaderCell(new Cell().add(new Paragraph("Contrôle / Critère").setBold().setFontSize(9)));
            matTable.addHeaderCell(new Cell().add(new Paragraph("Statut").setBold().setFontSize(9)));
            matTable.addHeaderCell(new Cell().add(new Paragraph("Score (0-5)").setBold().setFontSize(9)));
            matTable.addHeaderCell(new Cell().add(new Paragraph("Niveau de maturité").setBold().setFontSize(9)));

            for (Constat c : constats) {
                String subCrit = c.getControle().getSousCritere() != null ? c.getControle().getSousCritere() : "-";
                ResultatConstat res = c.getResultat();
                int score = getMaturityScore(res);
                
                matTable.addCell(new Cell().add(new Paragraph(subCrit).setFontSize(8)));
                matTable.addCell(new Cell().add(new Paragraph(c.getControle().getLibelle()).setFontSize(8)));
                matTable.addCell(new Cell().add(new Paragraph(res != null ? res.name() : "NON_EVALUE").setFontSize(8)));
                matTable.addCell(new Cell().add(new Paragraph(String.valueOf(score)).setFontSize(8)));
                matTable.addCell(new Cell().add(new Paragraph(getMaturityLabel(score)).setFontSize(8)));
            }
            doc.add(matTable);

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // --- 12. PLAN D'ACTION (ANNEXE 9) ---
            doc.add(new Paragraph("12. Plan d'action proposé (Annexe 9)").setFontSize(14).setBold());
            doc.add(new Paragraph("Tableau de synthèse des actions correctives convenues avec l'organisme audité pour traiter les vulnérabilités identifiées.").setFontSize(10));

            List<Action> actions = actionRepository.findActionsByMissionId(m.getId());
            if (actions.isEmpty()) {
                doc.add(new Paragraph("\nAucune action corrective planifiée pour cette mission (tous les contrôles sont conformes).").setFontSize(10).setItalic());
            } else {
                Table actionTable = new Table(5);
                actionTable.addHeaderCell(new Cell().add(new Paragraph("Projet / Contrôle").setBold().setFontSize(9)));
                actionTable.addHeaderCell(new Cell().add(new Paragraph("Action corrective").setBold().setFontSize(9)));
                actionTable.addHeaderCell(new Cell().add(new Paragraph("Priorité").setBold().setFontSize(9)));
                actionTable.addHeaderCell(new Cell().add(new Paragraph("Responsable").setBold().setFontSize(9)));
                actionTable.addHeaderCell(new Cell().add(new Paragraph("Échéance").setBold().setFontSize(9)));

                for (Action act : actions) {
                    actionTable.addCell(new Cell().add(new Paragraph(act.getConstat().getControle().getLibelle()).setFontSize(8)));
                    actionTable.addCell(new Cell().add(new Paragraph(act.getDescription()).setFontSize(8)));
                    actionTable.addCell(new Cell().add(new Paragraph(act.getPriorite() != null ? act.getPriorite().name() : "MOYENNE").setFontSize(8)));
                    actionTable.addCell(new Cell().add(new Paragraph(act.getResponsable() != null ? act.getResponsable() : "-").setFontSize(8)));
                    actionTable.addCell(new Cell().add(new Paragraph(act.getEcheance() != null ? act.getEcheance().toString() : "-").setFontSize(8)));
                }
                doc.add(actionTable);
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Échec de la génération du rapport PDF", e);
            throw new RuntimeException("Erreur de génération PDF", e);
        }
    }

    private byte[] buildDocxRapport(Mission m, List<Constat> constats, double compliance, String synthese,
                                    int version, String nomAuditeur, String numCertif, String contactAuditeur,
                                    String confidentialite, String historique) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // --- PAGE DE GARDE ---
            XWPFParagraph p1 = doc.createParagraph();
            p1.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r1 = p1.createRun();
            r1.setText("RÉPUBLIQUE TUNISIENNE");
            r1.setBold(true);
            r1.setFontSize(12);

            XWPFParagraph p2 = doc.createParagraph();
            p2.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r2 = p2.createRun();
            r2.setText("AGENCE NATIONALE DE CYBERSÉCURITÉ (ANCS)");
            r2.setBold(true);
            r2.setFontSize(12);

            addDocxParagraph(doc, "\n\n\n", false, 12, ParagraphAlignment.CENTER);
            
            XWPFParagraph pTitle = doc.createParagraph();
            pTitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitle = pTitle.createRun();
            rTitle.setText("RAPPORT D'AUDIT DE LA SÉCURITÉ DU SYSTÈME D'INFORMATION");
            rTitle.setBold(true);
            rTitle.setFontSize(16);

            addDocxParagraph(doc, "De : " + m.getOrganisme().getNom(), true, 14, ParagraphAlignment.CENTER);
            addDocxParagraph(doc, "\n\n", false, 12, ParagraphAlignment.CENTER);

            // Table de signatures
            XWPFTable coverTable = doc.createTable(2, 3);
            coverTable.getRow(0).getCell(0).setText("Expert Auditeur chargé de la mission");
            coverTable.getRow(0).getCell(1).setText("Cachet de l'auditeur");
            coverTable.getRow(0).getCell(2).setText("Signature");
            
            coverTable.getRow(1).getCell(0).setText(nomAuditeur + "\nN° Certif: " + numCertif + "\nContact: " + contactAuditeur);
            coverTable.getRow(1).getCell(1).setText("\n\n\n");
            coverTable.getRow(1).getCell(2).setText("\n\n\n");
            
            addDocxParagraph(doc, "\n\n", false, 10, ParagraphAlignment.LEFT);
            addDocxParagraph(doc, "Version : " + version, false, 9, ParagraphAlignment.LEFT);
            addDocxParagraph(doc, "Date : " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now()), false, 9, ParagraphAlignment.LEFT);
            addDocxParagraph(doc, "Diffusion : Document Confidentiel", true, 9, ParagraphAlignment.LEFT);

            // --- 1. AVANT-PROPOS ---
            addDocxHeading(doc, "1. Avant-propos", 14);
            addDocxHeading(doc, "1.1 Confidentialité du document", 12);
            addDocxParagraph(doc, confidentialite, false, 10, ParagraphAlignment.LEFT);

            addDocxHeading(doc, "1.2 Historique des modifications", 12);
            XWPFTable histTable = doc.createTable();
            XWPFTableRow headerRow = histTable.getRow(0);
            if (headerRow == null) headerRow = histTable.createRow();
            while (headerRow.getTableCells().size() < 4) headerRow.addNewTableCell();
            headerRow.getCell(0).setText("Version");
            headerRow.getCell(1).setText("Date");
            headerRow.getCell(2).setText("Auteur");
            headerRow.getCell(3).setText("Modifications");
            
            if (historique != null && !historique.trim().isEmpty()) {
                String[] histLines = historique.split("\n");
                for (String line : histLines) {
                    String[] parts = line.split(" - ");
                    XWPFTableRow row = histTable.createRow();
                    while (row.getTableCells().size() < 4) row.addNewTableCell();
                    if (parts.length >= 4) {
                        row.getCell(0).setText(parts[0]);
                        row.getCell(1).setText(parts[1]);
                        row.getCell(2).setText(parts[2]);
                        row.getCell(3).setText(parts[3]);
                    } else {
                        row.getCell(0).setText(line);
                        row.getCell(1).setText("-");
                        row.getCell(2).setText("-");
                        row.getCell(3).setText("-");
                    }
                }
            }

            addDocxHeading(doc, "1.3 Diffusion du document", 12);
            addDocxParagraph(doc, "Diffusion côté Expert Auditeur :", true, 10, ParagraphAlignment.LEFT);
            XWPFTable diffTable1 = doc.createTable(2, 3);
            diffTable1.getRow(0).getCell(0).setText("Nom & Prénom");
            diffTable1.getRow(0).getCell(1).setText("Titre");
            diffTable1.getRow(0).getCell(2).setText("Contact");
            diffTable1.getRow(1).getCell(0).setText(nomAuditeur);
            diffTable1.getRow(1).getCell(1).setText("Expert Auditeur Cybersécurité");
            diffTable1.getRow(1).getCell(2).setText(contactAuditeur);

            addDocxParagraph(doc, "Diffusion côté Organisme Audité :", true, 10, ParagraphAlignment.LEFT);
            XWPFTable diffTable2 = doc.createTable(2, 3);
            diffTable2.getRow(0).getCell(0).setText("Nom de l'organisme");
            diffTable2.getRow(0).getCell(1).setText("Contact RSSI");
            diffTable2.getRow(0).getCell(2).setText("Rôle");
            diffTable2.getRow(1).getCell(0).setText(m.getOrganisme().getNom());
            diffTable2.getRow(1).getCell(1).setText(m.getOrganisme().getContactRssiEmail() != null ? m.getOrganisme().getContactRssiEmail() : "-");
            diffTable2.getRow(1).getCell(2).setText("Responsable de la Sécurité des SI");

            // --- 2. CADRE DE LA MISSION ---
            addDocxHeading(doc, "2. Cadre de la mission", 14);
            addDocxParagraph(doc, "La présente mission d'audit est réalisée en application directe du décret-loi n° 2023-17 du 11 mars 2023 relatif à la cybersécurité, et de l'arrêté du ministre des technologies de la communication du 12 Septembre 2023, fixant les critères techniques d'audit et les modalités de suivi de la mise en œuvre des recommandations contenues dans le rapport d'audit.\n\n" +
                "Il s'agit d'un audit de sécurité réglementaire périodique visant à mesurer l'état de conformité du système d'information de l'organisme par rapport au référentiel ANCS et de proposer un plan d'action de remédiation adéquat.", false, 10, ParagraphAlignment.LEFT);

            // --- 3. TERMES ET DÉFINITIONS ---
            addDocxHeading(doc, "3. Termes et définitions", 14);
            addDocxParagraph(doc, "- Cybersécurité : État de protection du cyberespace assurant la confidentialité, l'intégrité et la disponibilité des données.\n" +
                "- Audit de sécurité : Processus systématique, indépendant et documenté permettant d'évaluer l'état de la sécurité par rapport à des critères d'audit.\n" +
                "- Constat : Résultat de l'évaluation des preuves d'audit par rapport aux critères d'audit.\n" +
                "- Plan d'action : Ensemble de mesures correctives ordonnées et planifiées visant à traiter les non-conformités.", false, 10, ParagraphAlignment.LEFT);

            // --- 4. RÉFÉRENCES ---
            addDocxHeading(doc, "4. Références", 14);
            addDocxParagraph(doc, "- Décret-loi n° 2023-17 du 11 mars 2023 relatif à la cybersécurité.\n" +
                "- Arrêté du 12 Septembre 2023 fixant les critères techniques d'audit ANCS.\n" +
                "- Norme de référence : " + m.getReferentiel().getNom() + " (Version: " + m.getReferentiel().getVersion() + ").", false, 10, ParagraphAlignment.LEFT);

            // --- 5. PRÉSENTATION DE L'ORGANISME ---
            addDocxHeading(doc, "5. Présentation de l'organisme", 14);
            XWPFTable orgInfoTable = doc.createTable(4, 2);
            orgInfoTable.getRow(0).getCell(0).setText("Nom de l'organisme");
            orgInfoTable.getRow(0).getCell(1).setText(m.getOrganisme().getNom());
            orgInfoTable.getRow(1).getCell(0).setText("Secteur d'activité");
            orgInfoTable.getRow(1).getCell(1).setText(m.getOrganisme().getSecteurActivite() != null ? m.getOrganisme().getSecteurActivite() : "Non spécifié");
            orgInfoTable.getRow(2).getCell(0).setText("Type d'obligation");
            orgInfoTable.getRow(2).getCell(1).setText(m.getOrganisme().getTypeObligationAudit() != null ? m.getOrganisme().getTypeObligationAudit() : "SOUMIS_AUDIT");
            orgInfoTable.getRow(3).getCell(0).setText("Adresse du siège");
            orgInfoTable.getRow(3).getCell(1).setText(m.getOrganisme().getAdresse() != null ? m.getOrganisme().getAdresse() : "Non spécifiée");

            // --- 6. CHAMP D'AUDIT ---
            addDocxHeading(doc, "6. Champ d'audit (Périmètre)", 14);
            addDocxHeading(doc, "6.1 Périmètre géographique", 12);
            addDocxParagraph(doc, "Les vérifications physiques et logiques couvrent le siège social et l'infrastructure d'hébergement de l'organisme situé à : " + (m.getOrganisme().getAdresse() != null ? m.getOrganisme().getAdresse() : "Siège principal") + ".", false, 10, ParagraphAlignment.LEFT);
            addDocxHeading(doc, "6.2 Périmètre logique", 12);
            addDocxParagraph(doc, "Périmètre technique ciblé : " + (m.getPerimetre() != null ? m.getPerimetre() : "L'ensemble du système d'information de production de l'organisme."), false, 10, ParagraphAlignment.LEFT);

            // --- 7. MÉTHODOLOGIE ---
            addDocxHeading(doc, "7. Méthodologie", 14);
            addDocxParagraph(doc, "L'audit a été mené selon la méthodologie standard de l'ANCS, comprenant :\n" +
                "- Des entretiens avec les équipes techniques et d'organisation (RSSI, administrateurs, etc.).\n" +
                "- Des revues documentaires (politiques de sécurité, procédures, configurations réseau).\n" +
                "- Des tests de conformité techniques et fonctionnels sur les postes de travail, serveurs et équipements de sécurité.\n\n" +
                "Équipe intervenante :\n" +
                "- " + nomAuditeur + " (Expert Auditeur certifié par l'ANCS — Numéro de certification: " + numCertif + ").", false, 10, ParagraphAlignment.LEFT);

            // --- 8. SYNTHÈSE DES RÉSULTATS ---
            addDocxHeading(doc, "8. Synthèse des résultats", 14);
            addDocxParagraph(doc, "Taux global de conformité : " + String.format("%.1f", compliance) + "%", true, 11, ParagraphAlignment.LEFT);
            addDocxParagraph(doc, "\nSynthèse exécutive :", true, 10, ParagraphAlignment.LEFT);
            addDocxParagraph(doc, synthese != null ? synthese : "Aucune synthèse rédigée.", false, 10, ParagraphAlignment.LEFT);

            // --- 9. PRÉSENTATION DÉTAILLÉE ---
            addDocxHeading(doc, "9. Présentation détaillée des résultats de l'audit", 14);
            addDocxParagraph(doc, "Les résultats de conformité sont présentés ci-dessous, groupés par domaine/critère du référentiel d'audit de sécurité conformément au paragraphe 9 du modèle ANCS.", false, 10, ParagraphAlignment.LEFT);

            Map<String, List<Constat>> groupedConstats = new LinkedHashMap<>();
            for (Constat c : constats) {
                String cat = c.getControle().getCategorie();
                if (cat == null || cat.trim().isEmpty()) {
                    cat = "Mesures générales";
                }
                groupedConstats.computeIfAbsent(cat, k -> new ArrayList<>()).add(c);
            }

            for (Map.Entry<String, List<Constat>> entry : groupedConstats.entrySet()) {
                addDocxHeading(doc, "Domaine : " + entry.getKey(), 12);
                for (Constat c : entry.getValue()) {
                    addDocxParagraph(doc, "Critère : " + c.getControle().getLibelle(), true, 10, ParagraphAlignment.LEFT);
                    addDocxParagraph(doc, "Résultat de l'évaluation : " + (c.getResultat() != null ? c.getResultat().name() : "NON_EVALUE"), false, 9, ParagraphAlignment.LEFT);
                    addDocxParagraph(doc, "Description / Commentaire : " + (c.getCommentaire() != null ? c.getCommentaire() : "Aucun commentaire."), false, 9, ParagraphAlignment.LEFT);

                    if (c.getResultat() == ResultatConstat.NON_CONFORME || c.getResultat() == ResultatConstat.OBSERVATION) {
                        String crit = c.getCriticite() != null ? c.getCriticite() : (c.getControle().getCriticite() != null ? c.getControle().getCriticite() : "MOYEN");
                        addDocxParagraph(doc, "  - Criticité : " + crit, false, 9, ParagraphAlignment.LEFT);
                        if (c.getPreuveDescription() != null && !c.getPreuveDescription().isEmpty()) {
                            addDocxParagraph(doc, "  - Preuve d'audit : " + c.getPreuveDescription(), false, 9, ParagraphAlignment.LEFT);
                        }
                        if (c.getComposantesImpactees() != null && !c.getComposantesImpactees().isEmpty()) {
                            addDocxParagraph(doc, "  - Composantes du SI impactées : " + c.getComposantesImpactees(), false, 9, ParagraphAlignment.LEFT);
                        }
                        if (c.getRecommandation() != null && !c.getRecommandation().isEmpty()) {
                            addDocxParagraph(doc, "  - Recommandation : " + c.getRecommandation(), true, 9, ParagraphAlignment.LEFT);
                        }
                    }
                    addDocxParagraph(doc, "--------------------------------------------------", false, 8, ParagraphAlignment.LEFT);
                }
            }

            // --- 10. APPRÉCIATION DES RISQUES ---
            addDocxHeading(doc, "10. Appréciation des risques", 14);
            addDocxParagraph(doc, "Conformément aux exigences de gestion des risques (ISO 27005 / EBIOS RM), les non-conformités identifiées engendrent les scénarios de risques prioritaires suivants :", false, 10, ParagraphAlignment.LEFT);
            
            int riskIndex = 1;
            for (Constat c : constats) {
                if (c.getResultat() == ResultatConstat.NON_CONFORME) {
                    String crit = c.getCriticite() != null ? c.getCriticite() : (c.getControle().getCriticite() != null ? c.getControle().getCriticite() : "MOYEN");
                    addDocxParagraph(doc, String.format("\nScénario du risque n°%d : Exploitation de la vulnérabilité liée à \"%s\"", riskIndex++, c.getControle().getLibelle()), true, 10, ParagraphAlignment.LEFT);
                    addDocxParagraph(doc, "  - Description : Menace d'accès non autorisé ou de compromission par manque de contrôle conforme.", false, 9, ParagraphAlignment.LEFT);
                    addDocxParagraph(doc, "  - Composante impactée : " + (c.getComposantesImpactees() != null ? c.getComposantesImpactees() : "Actifs SI liés"), false, 9, ParagraphAlignment.LEFT);
                    addDocxParagraph(doc, "  - Gravité du risque : " + crit, true, 9, ParagraphAlignment.LEFT);
                    addDocxParagraph(doc, "  - Recommandation associée : " + (c.getRecommandation() != null ? c.getRecommandation() : "Mettre en œuvre les correctifs"), false, 9, ParagraphAlignment.LEFT);
                }
            }

            // --- 11. MATURITÉ DES CONTRÔLES (ANNEXE 6) ---
            addDocxHeading(doc, "11. Tableau de maturité des contrôles (Annexe 6)", 14);
            addDocxParagraph(doc, "Évaluation de la maturité des contrôles de sécurité sur une échelle de 0 à 5 basée sur la conformité constatée (0: Inexistant, 1: Initial, 2: Répétable, 3: Défini, 4: Géré, 5: Optimisé).", false, 10, ParagraphAlignment.LEFT);
            
            XWPFTable matTable = doc.createTable(constats.size() + 1, 5);
            matTable.getRow(0).getCell(0).setText("Réf (Sous-critère)");
            matTable.getRow(0).getCell(1).setText("Contrôle / Critère");
            matTable.getRow(0).getCell(2).setText("Statut");
            matTable.getRow(0).getCell(3).setText("Score (0-5)");
            matTable.getRow(0).getCell(4).setText("Niveau de maturité");

            for (int i = 0; i < constats.size(); i++) {
                Constat c = constats.get(i);
                XWPFTableRow row = matTable.getRow(i + 1);
                String subCrit = c.getControle().getSousCritere() != null ? c.getControle().getSousCritere() : "-";
                ResultatConstat res = c.getResultat();
                int score = getMaturityScore(res);
                
                row.getCell(0).setText(subCrit);
                row.getCell(1).setText(c.getControle().getLibelle());
                row.getCell(2).setText(res != null ? res.name() : "NON_EVALUE");
                row.getCell(3).setText(String.valueOf(score));
                row.getCell(4).setText(getMaturityLabel(score));
            }

            // --- 12. PLAN D'ACTION (ANNEXE 9) ---
            addDocxHeading(doc, "12. Plan d'action proposé (Annexe 9)", 14);
            addDocxParagraph(doc, "Tableau de synthèse des actions correctives convenues avec l'organisme audité pour traiter les vulnérabilités identifiées.", false, 10, ParagraphAlignment.LEFT);

            List<Action> actions = actionRepository.findActionsByMissionId(m.getId());
            if (actions.isEmpty()) {
                addDocxParagraph(doc, "\nAucune action corrective planifiée pour cette mission (tous les contrôles sont conformes).", false, 10, ParagraphAlignment.LEFT);
            } else {
                XWPFTable actionTable = doc.createTable(actions.size() + 1, 5);
                actionTable.getRow(0).getCell(0).setText("Projet / Contrôle");
                actionTable.getRow(0).getCell(1).setText("Action corrective");
                actionTable.getRow(0).getCell(2).setText("Priorité");
                actionTable.getRow(0).getCell(3).setText("Responsable");
                actionTable.getRow(0).getCell(4).setText("Échéance");

                for (int i = 0; i < actions.size(); i++) {
                    Action act = actions.get(i);
                    XWPFTableRow row = actionTable.getRow(i + 1);
                    row.getCell(0).setText(act.getConstat().getControle().getLibelle());
                    row.getCell(1).setText(act.getDescription());
                    row.getCell(2).setText(act.getPriorite() != null ? act.getPriorite().name() : "MOYENNE");
                    row.getCell(3).setText(act.getResponsable() != null ? act.getResponsable() : "-");
                    row.getCell(4).setText(act.getEcheance() != null ? act.getEcheance().toString() : "-");
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Échec de la génération du rapport Word (DOCX)", e);
            throw new RuntimeException("Erreur de génération DOCX", e);
        }
    }

    private void addDocxParagraph(XWPFDocument doc, String text, boolean bold, int size, ParagraphAlignment alignment) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alignment);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(size);
    }

    private void addDocxHeading(XWPFDocument doc, String text, int size) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setText("\n" + text);
        r.setBold(true);
        r.setFontSize(size);
    }

    private int getMaturityScore(ResultatConstat res) {
        if (res == null) return 0;
        switch (res) {
            case CONFORME: return 5;
            case OBSERVATION: return 3;
            case NON_CONFORME: return 1;
            default: return 0;
        }
    }

    private String getMaturityLabel(int score) {
        switch (score) {
            case 5: return "Optimise (Conforme)";
            case 4: return "Gere";
            case 3: return "Defini (Observation)";
            case 2: return "Repetable";
            case 1: return "Initial (Non Conforme)";
            default: return "Inexistant / Non evalue";
        }
    }

    @Transactional
    @AuditAction(action = "SUBMIT_RAPPORT", resource = "RAPPORT", extractResourceId = true)
    public Rapport submitRapport(UUID rapportId, String userEmail, Role userRole) {
        Rapport rapport = rapportRepository.findById(rapportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouve avec l'id : " + rapportId));

        boolean isAssignedAuditor = rapport.getMission().getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(userEmail);
        boolean isAdmin = userRole == Role.ADMIN_ANCS;
        if (!isAssignedAuditor && !isAdmin) {
            throw new AccessDeniedException("Acces refuse");
        }

        rapport.setStatutSoumissionAncs(StatutSoumissionAncs.SOUMIS);
        rapport.setDateSoumissionAncs(Instant.now());
        return rapportRepository.save(rapport);
    }

    @Transactional
    @AuditAction(action = "ACCEPT_RAPPORT", resource = "RAPPORT", extractResourceId = true)
    public Rapport acceptRapport(UUID rapportId) {
        Rapport rapport = rapportRepository.findById(rapportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouve avec l'id : " + rapportId));

        rapport.setStatutSoumissionAncs(StatutSoumissionAncs.ACCEPTE);
        return rapportRepository.save(rapport);
    }

    @Transactional
    @AuditAction(action = "REJECT_RAPPORT", resource = "RAPPORT", extractResourceId = true)
    public Rapport rejectRapport(UUID rapportId, String motifRejet) {
        Rapport rapport = rapportRepository.findById(rapportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouve avec l'id : " + rapportId));

        rapport.setStatutSoumissionAncs(StatutSoumissionAncs.REJETE);
        rapport.setMotifRejet(motifRejet);
        rapport.setDateLimiteResoumission(LocalDate.now().plusMonths(2));
        return rapportRepository.save(rapport);
    }
}
