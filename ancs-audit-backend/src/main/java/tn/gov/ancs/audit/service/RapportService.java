package tn.gov.ancs.audit.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wickedsource.docxstamper.DocxStamper;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import tn.gov.ancs.audit.domain.*;
import tn.gov.ancs.audit.domain.enums.ResultatConstat;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.domain.enums.StatutSoumissionAncs;
import tn.gov.ancs.audit.dto.request.AiSummaryRequest;
import tn.gov.ancs.audit.dto.response.RapportResponse;
import tn.gov.ancs.audit.dto.response.SyntheseIaResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.*;
import tn.gov.ancs.audit.security.AuditAction;
import tn.gov.ancs.audit.service.ai.AiSummaryService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
        if (constats.isEmpty()) {
            throw new IllegalArgumentException("Impossible de générer la synthèse IA car aucun constat n'est présent pour cette mission. Veuillez d'abord initialiser et remplir la grille d'évaluation.");
        }
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
        if (syntheseExecutive == null || syntheseExecutive.trim().length() < 50) {
            throw new IllegalArgumentException("La synthèse exécutive doit contenir au moins 50 caractères.");
        }
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
        if (constats.isEmpty()) {
            throw new IllegalArgumentException("Impossible de générer le rapport car aucun constat n'est présent pour cette mission. Veuillez d'abord initialiser et remplir la grille d'évaluation.");
        }
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
        String currentHistEntry = "Version " + nextVersion + "|||" + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now()) + "|||" + nomAuditeur + "|||Génération du rapport";
        String historique = previousRapportOpt.isPresent() && previousRapportOpt.get().getHistoriqueVersions() != null
            ? previousRapportOpt.get().getHistoriqueVersions() + "\n" + currentHistEntry
            : currentHistEntry;

        byte[] fileBytes = buildRapportFromTemplate(mission, constats, compliance, syntheseExecutive, nextVersion, nomAuditeur, numCertif, contactAuditeur, historique, type);
        String mimeType = "PDF".equalsIgnoreCase(type) ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        objectName = storageService.uploadRapport(filename, new ByteArrayInputStream(fileBytes), mimeType);

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
    // Moteur de rendu docx-stamper
    // ========================================================

    @Data
    public static class RapportContext {
        private OrganismeContext organisme;
        private AuditeurContext auditeur;
        private RapportInfoContext rapport;
        private List<ConstatContext> constats;
        private List<ActionContext> actions;
        private List<MaturiteContext> maturite;
        private List<HistoriqueContext> historique;
    }

    @Data public static class OrganismeContext {
        private String nom;
        private String acronyme;
        private String statut;
        private String categorie;
        private String adresse;
        private String siteWeb;
    }

    @Data public static class AuditeurContext {
        private String nomComplet;
        private String numeroCertif;
        private String email;
    }

    @Data public static class RapportInfoContext {
        private String version;
        private String dateGeneration;
        private String tauxConformite;
        private String syntheseExecutive;
    }

    @Data public static class ConstatContext {
        private String domaine;
        private String critere;
        private String resultat;
        private String commentaire;
        private String criticite;
    }

    @Data public static class ActionContext {
        private String controle;
        private String description;
        private String priorite;
        private String responsable;
        private String echeance;
    }

    @Data public static class MaturiteContext {
        private String sousCritere;
        private String critere;
        private String resultat;
        private String score;
        private String niveau;
    }

    @Data public static class HistoriqueContext {
        private String version;
        private String date;
        private String auteur;
        private String modifications;
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

    private String safe(String value) {
        return value != null ? value : "";
    }

    private byte[] buildRapportFromTemplate(Mission m, List<Constat> constats, double compliance, String synthese,
                                            int version, String nomAuditeur, String numCertif, String contactAuditeur,
                                            String historiqueStr, String type) {
        RapportContext context = new RapportContext();
        
        // Organisme
        OrganismeContext org = new OrganismeContext();
        org.setNom(safe(m.getOrganisme() != null ? m.getOrganisme().getNom() : null));
        org.setAcronyme(safe(m.getOrganisme() != null ? m.getOrganisme().getAcronyme() : null));
        org.setStatut(safe(m.getOrganisme() != null ? m.getOrganisme().getStatut() : null));
        org.setCategorie(safe(m.getOrganisme() != null ? m.getOrganisme().getCategorie() : null));
        org.setAdresse(safe(m.getOrganisme() != null ? m.getOrganisme().getAdresse() : null));
        org.setSiteWeb("");
        context.setOrganisme(org);

        // Auditeur
        AuditeurContext aud = new AuditeurContext();
        aud.setNomComplet(safe(nomAuditeur));
        aud.setNumeroCertif(safe(numCertif));
        aud.setEmail(safe(contactAuditeur));
        context.setAuditeur(aud);

        // Rapport Info
        RapportInfoContext rap = new RapportInfoContext();
        rap.setVersion(safe(String.valueOf(version)));
        rap.setDateGeneration(safe(DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now())));
        rap.setTauxConformite(safe(String.format("%.1f%%", compliance)));
        rap.setSyntheseExecutive(safe(synthese));
        context.setRapport(rap);

        // Historique
        List<HistoriqueContext> histList = new ArrayList<>();
        if (historiqueStr != null && !historiqueStr.trim().isEmpty()) {
            String[] histLines = historiqueStr.split("\n");
            for (String line : histLines) {
                String[] parts = line.contains("|||") ? line.split("\\|\\|\\|") : line.split(" - ");
                HistoriqueContext hc = new HistoriqueContext();
                if (parts.length >= 4) {
                    hc.setVersion(safe(parts[0]));
                    hc.setDate(safe(parts[1]));
                    hc.setAuteur(safe(parts[2]));
                    hc.setModifications(safe(parts[3]));
                } else {
                    hc.setVersion(safe(line));
                    hc.setDate("");
                    hc.setAuteur("");
                    hc.setModifications("");
                }
                histList.add(hc);
            }
        }
        context.setHistorique(histList);

        // Constats et Maturité
        List<ConstatContext> cList = new ArrayList<>();
        List<MaturiteContext> mList = new ArrayList<>();
        for (Constat c : constats) {
            ConstatContext cc = new ConstatContext();
            String cat = (c.getControle() != null) ? c.getControle().getCategorie() : null;
            cc.setDomaine(safe(cat));
            cc.setCritere(safe(c.getControle() != null ? c.getControle().getLibelle() : null));
            cc.setResultat(safe(c.getResultat() != null ? c.getResultat().name() : null));
            cc.setCommentaire(safe(c.getCommentaire()));
            String crit = c.getCriticite() != null ? c.getCriticite() : (c.getControle() != null ? c.getControle().getCriticite() : null);
            cc.setCriticite(safe(crit));
            cList.add(cc);

            MaturiteContext mc = new MaturiteContext();
            mc.setSousCritere(safe(c.getControle() != null ? c.getControle().getSousCritere() : null));
            mc.setCritere(safe(c.getControle() != null ? c.getControle().getLibelle() : null));
            mc.setResultat(safe(c.getResultat() != null ? c.getResultat().name() : null));
            int score = getMaturityScore(c.getResultat());
            mc.setScore(safe(String.valueOf(score)));
            mc.setNiveau(safe(getMaturityLabel(score)));
            mList.add(mc);
        }
        context.setConstats(cList);
        context.setMaturite(mList);

        // Actions
        List<Action> actions = actionRepository.findActionsByMissionId(m.getId());
        List<ActionContext> aList = new ArrayList<>();
        for (Action act : actions) {
            ActionContext ac = new ActionContext();
            String ctrlLib = (act.getConstat() != null && act.getConstat().getControle() != null)
                ? act.getConstat().getControle().getLibelle() : null;
            ac.setControle(safe(ctrlLib));
            ac.setDescription(safe(act.getDescription()));
            ac.setPriorite(safe(act.getPriorite() != null ? act.getPriorite().name() : null));
            ac.setResponsable(safe(act.getResponsable()));
            ac.setEcheance(safe(act.getEcheance() != null ? act.getEcheance().toString() : null));
            aList.add(ac);
        }
        context.setActions(aList);

        Path tempDocx = null;
        Path tempPdf = null;
        try {
            tempDocx = Files.createTempFile("rapport_", ".docx");
            try (InputStream templateStream = new ClassPathResource("templates/modele_rapport_ancs_2_1.docx").getInputStream();
                 OutputStream out = Files.newOutputStream(tempDocx)) {
                WordprocessingMLPackage document = WordprocessingMLPackage.load(templateStream);
                var stamper = new DocxStamperConfiguration()
                    .setFailOnUnresolvedExpression(false)
                    .build();
                stamper.stamp(document, context, out);
            }

            if ("PDF".equalsIgnoreCase(type)) {
                String profileDir = "file:///tmp/lo_profile_" + UUID.randomUUID().toString();
                ProcessBuilder pb = new ProcessBuilder("soffice",
                        "-env:UserInstallation=" + profileDir,
                        "--headless",
                        "--convert-to", "pdf",
                        "--outdir", tempDocx.getParent().toString(),
                        tempDocx.toString());
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                // Consume output stream to prevent hang
                try (InputStream is = process.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {
                        // ignore output
                    }
                }
                
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException("Conversion LibreOffice a dépassé le délai de 30 secondes.");
                }
                
                tempPdf = tempDocx.getParent().resolve(tempDocx.getFileName().toString().replace(".docx", ".pdf"));
                byte[] pdfBytes = Files.readAllBytes(tempPdf);
                return pdfBytes;
            } else {
                byte[] docxBytes = Files.readAllBytes(tempDocx);
                return docxBytes;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur de génération", e);
        } finally {
            if (tempDocx != null) {
                try {
                    Files.deleteIfExists(tempDocx);
                } catch (Exception ex) {
                    log.warn("Impossible de supprimer le fichier temporaire DOCX : {}", tempDocx, ex);
                }
            }
            if (tempPdf != null) {
                try {
                    Files.deleteIfExists(tempPdf);
                } catch (Exception ex) {
                    log.warn("Impossible de supprimer le fichier temporaire PDF : {}", tempPdf, ex);
                }
            }
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

    @Transactional(readOnly = true)
    public List<RapportResponse> getRapportsByMission(UUID missionId, String auditorEmail) {
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mission non trouvée"));
        
        Utilisateur caller = utilisateurRepository.findByEmailIgnoreCase(auditorEmail)
            .orElseThrow(() -> new AccessDeniedException("Accès refusé"));

        boolean isAssignedAuditor = mission.getAuditeur().getUtilisateur().getEmail().equalsIgnoreCase(auditorEmail);
        boolean isAdmin = caller.getRole() == Role.ADMIN_ANCS;

        if (!isAssignedAuditor && !isAdmin) {
            throw new AccessDeniedException("Accès refusé");
        }

        List<Rapport> rapports = rapportRepository.findByMissionIdOrderByVersionDesc(missionId);
        return rapports.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RapportResponse> getMyOrganismeRapports(String rssiEmail) {
        Utilisateur caller = utilisateurRepository.findByEmailIgnoreCase(rssiEmail)
            .orElseThrow(() -> new AccessDeniedException("Accès refusé"));
            
        if (caller.getRole() != Role.RSSI && caller.getRole() != Role.ADMIN_ANCS) {
            throw new AccessDeniedException("Accès refusé");
        }
        
        List<Rapport> rapports;
        if (caller.getRole() == Role.ADMIN_ANCS) {
             rapports = rapportRepository.findAll();
        } else {
             if (caller.getOrganisme() == null) {
                 return Collections.emptyList();
             }
             rapports = rapportRepository.findByOrganismeId(caller.getOrganisme().getId());
        }
        return rapports.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private RapportResponse mapToResponse(Rapport rapport) {
        return RapportResponse.builder()
            .id(rapport.getId())
            .missionId(rapport.getMission().getId())
            .organismeNom(rapport.getMission().getOrganisme().getNom())
            .auditeurNom(rapport.getNomAuditeur() != null ? rapport.getNomAuditeur() : rapport.getMission().getAuditeur().getUtilisateur().getNom())
            .type(rapport.getType())
            .version(rapport.getVersion())
            .dateGeneration(rapport.getDateGeneration())
            .syntheseGenereeParIa(Boolean.TRUE.equals(rapport.getSyntheseGenereeParIa()))
            .statutSoumissionAncs(rapport.getStatutSoumissionAncs() != null ? rapport.getStatutSoumissionAncs().name() : "NON_SOUMIS")
            .motifRejet(rapport.getMotifRejet())
            .numeroCertificationAncs(rapport.getNumeroCertificationAncs())
            .contactAuditeur(rapport.getContactAuditeur())
            .build();
    }
}
