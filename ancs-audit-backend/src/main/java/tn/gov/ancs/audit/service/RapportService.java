package tn.gov.ancs.audit.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wickedsource.docxstamper.DocxStamperConfiguration;
import tn.gov.ancs.audit.domain.*;
import tn.gov.ancs.audit.domain.enums.PrioriteAction;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        // TODO: Re-enable minimum length validation before production
        // if (syntheseExecutive == null || syntheseExecutive.trim().length() < 50) {
        //     throw new IllegalArgumentException("La synthèse exécutive doit contenir au moins 50 caractères.");
        // }
        if (syntheseExecutive == null || syntheseExecutive.trim().isEmpty()) {
            throw new IllegalArgumentException("La synthèse exécutive ne peut pas être vide.");
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

        String filename = "Rapport_Audit_" + mission.getOrganisme().getNom().replace(" ", "_") + ".docx";
        
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

        byte[] fileBytes = buildRapportFromTemplate(mission, constats, compliance, syntheseExecutive, nextVersion, historique);
        String mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String objectName = storageService.uploadRapport(filename, fileBytes, mimeType);

        // Générer et téléverser le document résumé (points clés)
        List<Action> actions = actionRepository.findActionsByMissionId(mission.getId());
        String resumeFilename = "Resume_Audit_" + mission.getOrganisme().getNom().replace(" ", "_") + ".docx";
        byte[] resumeBytes = buildResumeDocx(mission, constats, compliance, syntheseExecutive, nextVersion, actions);
        String resumeObjectName = storageService.uploadRapport(resumeFilename, resumeBytes, mimeType);

        // Créer l'entité Rapport
        Rapport rapport = Rapport.builder()
            .mission(mission)
            .fichierUrl(objectName)
            .fichierResumeUrl(resumeObjectName)
            .dateGeneration(Instant.now())
            .version(nextVersion)
            .type("DOCX")
            .syntheseGenereeParIa(isIaGenerated)
            .syntheseIaHorodatage(isIaGenerated ? Instant.now() : null)
            .nomAuditeur(nomAuditeur)
            .numeroCertificationAncs(numCertif)
            .contactAuditeur(contactAuditeur)
            .texteConfidentialite(confidentialite)
            .historiqueVersions(historique)
            .build();

        return rapportRepository.save(rapport);
    }

    /**
     * Vérifie que l'utilisateur est autorisé à accéder au rapport demandé.
     * Factorisé pour éviter toute divergence entre les deux points de téléchargement.
     *
     * @throws AccessDeniedException si l'accès est refusé.
     */
    private void checkDownloadAccess(Rapport rapport, String userEmail, Role userRole, UUID userOrganismeId) {
        // Contrôle d'accès : le RSSI ne peut télécharger que les rapports de son organisme.
        if (userRole == Role.RSSI) {
            UUID targetOrgId = rapport.getMission().getOrganisme().getId();
            if (!targetOrgId.equals(userOrganismeId)) {
                throw new AccessDeniedException("Accès refusé : vous n'avez pas l'autorisation d'accéder au rapport d'un autre organisme");
            }
        }
        // Contrôle d'accès : un AUDITEUR ne peut télécharger que les rapports
        // des missions qui lui sont assignées — empêche l'accès IDOR.
        if (userRole == Role.AUDITEUR) {
            String assignedEmail = rapport.getMission().getAuditeur().getUtilisateur().getEmail();
            if (!assignedEmail.equalsIgnoreCase(userEmail)) {
                throw new AccessDeniedException("Accès refusé : vous n'êtes pas l'auditeur assigné à cette mission");
            }
        }
    }

    /**
     * Récupère le lien de téléchargement sécurisé pré-signé pour un rapport d'audit.
     */
    @Transactional(readOnly = true)
    @AuditAction(action = "DOWNLOAD_RAPPORT", resource = "RAPPORT", extractResourceId = true)
    public String getRapportDownloadUrl(UUID rapportId, String userEmail, Role userRole, UUID userOrganismeId) {
        Rapport rapport = rapportRepository.findById(rapportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        checkDownloadAccess(rapport, userEmail, userRole, userOrganismeId);

        return storageService.getPresignedUrl(storageService.getRapportsBucket(), rapport.getFichierUrl());
    }

    /**
     * Récupère le lien de téléchargement sécurisé pré-signé pour le résumé d'un rapport.
     * Applique les mêmes contrôles d'accès IDOR que {@link #getRapportDownloadUrl}.
     *
     * @throws ResourceNotFoundException si aucun résumé n'a été généré (rapport antérieur à la fonctionnalité).
     */
    @Transactional(readOnly = true)
    @AuditAction(action = "DOWNLOAD_RAPPORT_RESUME", resource = "RAPPORT", extractResourceId = true)
    public String getRapportResumeDownloadUrl(UUID rapportId, String userEmail, Role userRole, UUID userOrganismeId) {
        Rapport rapport = rapportRepository.findById(rapportId)
            .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        checkDownloadAccess(rapport, userEmail, userRole, userOrganismeId);

        if (rapport.getFichierResumeUrl() == null) {
            throw new ResourceNotFoundException("Aucun résumé disponible pour ce rapport");
        }
        return storageService.getPresignedUrl(storageService.getRapportsBucket(), rapport.getFichierResumeUrl());
    }

    // ========================================================
    // Moteur de rendu docx-stamper
    // ========================================================

    @Data
    public static class RapportContext {
        private OrganismeContext organisme;
        private RssiContext rssi;
        private AuditeurContext auditeur;
        private MissionContext mission;
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
        private String secteurActivite;
        private String typeObligationAudit;
        private String contactRssiEmail;
    }

    /** RSSI désigné de l'organisme audité. */
    @Data public static class RssiContext {
        private String nomComplet;
        private String email;
    }

    @Data public static class AuditeurContext {
        private String nomComplet;
        private String numeroCertif;
        private String email;
        private String dateCertification;
        private String dateExpiration;
        private String specialites;
    }

    @Data public static class MissionContext {
        private String dateDebut;
        private String dateFin;
        private String perimetre;
        private String referentielNom;
        private String referentielVersion;
        private String statut;
    }

    @Data public static class RapportInfoContext {
        private String version;
        private String dateGeneration;
        private String tauxConformite;
        private String syntheseExecutive;
        private String texteConfidentialite;
    }

    @Data public static class ConstatContext {
        private String domaine;
        private String sousDomaine;
        private String critere;
        private String resultat;
        private String commentaire;
        private String criticite;
        private String recommandation;
        private String composantesImpactees;
        private String preuveDescription;
        private String dateConstat;
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

    private byte[] buildRapportFromTemplate(Mission m, List<Constat> constats, double compliance,
                                            String synthese, int version, String historiqueStr) {
        RapportContext context = new RapportContext();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // ── Organisme ──────────────────────────────────────────────
        Organisme org = m.getOrganisme();
        OrganismeContext orgCtx = new OrganismeContext();
        if (org != null) {
            orgCtx.setNom(safe(org.getNom()));
            orgCtx.setAcronyme(safe(org.getAcronyme()));
            orgCtx.setStatut(safe(org.getStatut()));
            orgCtx.setCategorie(safe(org.getCategorie()));
            orgCtx.setAdresse(safe(org.getAdresse()));
            orgCtx.setSecteurActivite(safe(org.getSecteurActivite()));
            orgCtx.setTypeObligationAudit(safe(org.getTypeObligationAudit()));
            orgCtx.setContactRssiEmail(safe(org.getContactRssiEmail()));
        }
        context.setOrganisme(orgCtx);

        // ── RSSI de l'organisme ────────────────────────────────────
        RssiContext rssiCtx = new RssiContext();
        if (org != null) {
            utilisateurRepository.findByOrganismeId(org.getId()).stream()
                .filter(u -> u.getRole() == Role.RSSI)
                .findFirst()
                .ifPresent(rssi -> {
                    rssiCtx.setNomComplet(safe(rssi.getNom()));
                    rssiCtx.setEmail(safe(rssi.getEmail()));
                });
        }
        context.setRssi(rssiCtx);

        // ── Auditeur ───────────────────────────────────────────────
        Auditeur auditeur = m.getAuditeur();
        AuditeurContext audCtx = new AuditeurContext();
        if (auditeur != null) {
            audCtx.setNomComplet(safe(auditeur.getUtilisateur().getNom()));
            audCtx.setNumeroCertif(safe(auditeur.getNumeroCertification()));
            audCtx.setEmail(safe(auditeur.getUtilisateur().getEmail()));
            audCtx.setDateCertification(auditeur.getDateCertification() != null
                ? fmt.format(auditeur.getDateCertification()) : "");
            audCtx.setDateExpiration(auditeur.getDateExpiration() != null
                ? fmt.format(auditeur.getDateExpiration()) : "");
            audCtx.setSpecialites(auditeur.getSpecialites() != null
                ? String.join(", ", auditeur.getSpecialites()) : "");
        }
        context.setAuditeur(audCtx);

        // ── Mission ────────────────────────────────────────────────
        MissionContext missionCtx = new MissionContext();
        missionCtx.setDateDebut(m.getDateDebut() != null ? fmt.format(m.getDateDebut()) : "");
        missionCtx.setDateFin(m.getDateFin() != null ? fmt.format(m.getDateFin()) : "");
        missionCtx.setPerimetre(safe(m.getPerimetre()));
        missionCtx.setStatut(m.getStatut() != null ? m.getStatut().name() : "");
        if (m.getReferentiel() != null) {
            missionCtx.setReferentielNom(safe(m.getReferentiel().getNom()));
            missionCtx.setReferentielVersion(safe(m.getReferentiel().getVersion()));
        }
        context.setMission(missionCtx);

        // ── Rapport Info ───────────────────────────────────────────
        String confidentialite =
            "Le présent document est confidentiel et sa confidentialité consiste à :\n" +
            "- La non divulgation desdites informations confidentielles auprès de tierce partie,\n" +
            "- La non reproduction des informations dites confidentielles, sauf accord de l'organisme audité,\n" +
            "- Ne pas profiter ou faire profiter tierce partie du contenu de ces informations en matière de savoir-faire,\n" +
            "- Considérer toutes les informations relatives à la production et au système d'information de l'organisme audité déclarées Confidentielles.";
        RapportInfoContext rapCtx = new RapportInfoContext();
        rapCtx.setVersion(String.valueOf(version));
        rapCtx.setDateGeneration(fmt.format(LocalDate.now()));
        rapCtx.setTauxConformite(String.format("%.1f%%", compliance));
        rapCtx.setSyntheseExecutive(safe(synthese));
        rapCtx.setTexteConfidentialite(confidentialite);
        context.setRapport(rapCtx);

        // ── Historique ─────────────────────────────────────────────
        List<HistoriqueContext> histList = new ArrayList<>();
        if (historiqueStr != null && !historiqueStr.trim().isEmpty()) {
            for (String line : historiqueStr.split("\n")) {
                String[] parts = line.contains("|||") ? line.split("\\|\\|\\|") : line.split(" - ");
                HistoriqueContext hc = new HistoriqueContext();
                if (parts.length >= 4) {
                    hc.setVersion(safe(parts[0]));
                    hc.setDate(safe(parts[1]));
                    hc.setAuteur(safe(parts[2]));
                    hc.setModifications(safe(parts[3]));
                } else {
                    hc.setVersion(safe(line));
                    hc.setDate(""); hc.setAuteur(""); hc.setModifications("");
                }
                histList.add(hc);
            }
        }
        context.setHistorique(histList);

        // ── Constats & Maturité ────────────────────────────────────
        List<ConstatContext> cList = new ArrayList<>();
        List<MaturiteContext> mList = new ArrayList<>();
        for (Constat c : constats) {
            ConstatContext cc = new ConstatContext();
            cc.setDomaine(safe(c.getControle() != null ? c.getControle().getCategorie() : null));
            cc.setSousDomaine(safe(c.getControle() != null ? c.getControle().getSousCritere() : null));
            cc.setCritere(safe(c.getControle() != null ? c.getControle().getLibelle() : null));
            cc.setResultat(safe(c.getResultat() != null ? c.getResultat().name() : null));
            cc.setCommentaire(safe(c.getCommentaire()));
            String crit = c.getCriticite() != null ? c.getCriticite()
                : (c.getControle() != null ? c.getControle().getCriticite() : null);
            cc.setCriticite(safe(crit));
            cc.setRecommandation(safe(c.getRecommandation()));
            cc.setComposantesImpactees(safe(c.getComposantesImpactees()));
            cc.setPreuveDescription(safe(c.getPreuveDescription()));
            cc.setDateConstat(c.getDateConstat() != null
                ? fmt.format(c.getDateConstat().atZone(java.time.ZoneOffset.UTC).toLocalDate()) : "");
            cList.add(cc);

            MaturiteContext mc = new MaturiteContext();
            mc.setSousCritere(safe(c.getControle() != null ? c.getControle().getSousCritere() : null));
            mc.setCritere(safe(c.getControle() != null ? c.getControle().getLibelle() : null));
            mc.setResultat(safe(c.getResultat() != null ? c.getResultat().name() : null));
            int score = getMaturityScore(c.getResultat());
            mc.setScore(String.valueOf(score));
            mc.setNiveau(getMaturityLabel(score));
            mList.add(mc);
        }
        context.setConstats(cList);
        context.setMaturite(mList);

        // ── Actions correctives ────────────────────────────────────
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

        // ── Stamper DOCX ───────────────────────────────────────────
        Path tempDocx = null;
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
            return Files.readAllBytes(tempDocx);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport DOCX", e);
            throw new RuntimeException("Erreur de génération du rapport", e);
        } finally {
            if (tempDocx != null) {
                try { Files.deleteIfExists(tempDocx); }
                catch (Exception ex) { log.warn("Impossible de supprimer le fichier temporaire : {}", tempDocx, ex); }
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
            .resumeDisponible(rapport.getFichierResumeUrl() != null)
            .build();
    }

    // ========================================================
    // Générateur de résumé (points clés) — Apache POI direct
    // ========================================================

    /**
     * Construit un document Word d'une à deux pages résumant les points clés du rapport.
     *
     * <p>Contenu :
     * <ol>
     *   <li>En-tête : organisme, référentiel, dates de mission, auditeur</li>
     *   <li>Informations rapport : version, date de génération, statut ANCS</li>
     *   <li>Taux de conformité global</li>
     *   <li>Répartition des constats par résultat</li>
     *   <li>Non-conformités critiques (résultat NON_CONFORME + criticité ELEVE ou CRITIQUE)</li>
     *   <li>Synthèse exécutive</li>
     *   <li>Actions correctives prioritaires (priorité HAUTE ou CRITIQUE)</li>
     * </ol>
     */
    private byte[] buildResumeDocx(Mission mission, List<Constat> constats, double compliance,
                                   String synthese, int version, List<Action> actions) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            Auditeur auditeur = mission.getAuditeur();
            Organisme org = mission.getOrganisme();

            // ── Titre principal ────────────────────────────────────────
            addHeading(doc, "RÉSUMÉ DU RAPPORT D'AUDIT", 1);
            addHeading(doc, safe(org != null ? org.getNom() : ""), 2);

            // ── Section 1 : Informations générales ─────────────────────
            addHeading(doc, "1. Informations générales", 2);
            addBullet(doc, "Organisme : " + safe(org != null ? org.getNom() : ""));
            if (mission.getReferentiel() != null) {
                addBullet(doc, "Référentiel : " + safe(mission.getReferentiel().getNom())
                    + " v" + safe(mission.getReferentiel().getVersion()));
            }
            addBullet(doc, "Période : "
                + (mission.getDateDebut() != null ? fmt.format(mission.getDateDebut()) : "?")
                + " → "
                + (mission.getDateFin() != null ? fmt.format(mission.getDateFin()) : "?"));
            if (auditeur != null) {
                addBullet(doc, "Auditeur : " + safe(auditeur.getUtilisateur().getNom())
                    + "  •  N° " + safe(auditeur.getNumeroCertification()));
            }

            // ── Section 2 : Informations rapport ───────────────────────
            addHeading(doc, "2. Informations du rapport", 2);
            addBullet(doc, "Version : " + version);
            addBullet(doc, "Date de génération : " + fmt.format(LocalDate.now()));

            // ── Section 3 : Taux de conformité ─────────────────────────
            addHeading(doc, "3. Taux de conformité global", 2);
            XWPFParagraph compPara = doc.createParagraph();
            compPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun compRun = compPara.createRun();
            compRun.setBold(true);
            compRun.setFontSize(28);
            compRun.setText(String.format("%.1f%%", compliance));

            // ── Section 4 : Répartition des constats ───────────────────
            addHeading(doc, "4. Répartition des constats", 2);
            long nbConforme   = constats.stream().filter(c -> c.getResultat() == ResultatConstat.CONFORME).count();
            long nbObservation = constats.stream().filter(c -> c.getResultat() == ResultatConstat.OBSERVATION).count();
            long nbNonConforme = constats.stream().filter(c -> c.getResultat() == ResultatConstat.NON_CONFORME).count();
            addBullet(doc, "Conforme         : " + nbConforme);
            addBullet(doc, "Observation      : " + nbObservation);
            addBullet(doc, "Non conforme     : " + nbNonConforme);
            addBullet(doc, "Total évalués    : " + constats.size());

            // Répartition par criticité
            Map<String, Long> parCriticite = constats.stream()
                .filter(c -> c.getResultat() != null)
                .collect(Collectors.groupingBy(
                    c -> {
                        String crit = c.getCriticite() != null ? c.getCriticite()
                            : (c.getControle() != null ? c.getControle().getCriticite() : null);
                        return crit != null ? crit : "N/A";
                    },
                    Collectors.counting()
                ));
            if (!parCriticite.isEmpty()) {
                addBullet(doc, "— par criticité :");
                parCriticite.forEach((k, v) -> addBullet(doc, "    " + k + " : " + v));
            }

            // ── Section 5 : Non-conformités critiques ──────────────────
            addHeading(doc, "5. Non-conformités critiques", 2);
            List<Constat> critiques = constats.stream()
                .filter(c -> c.getResultat() == ResultatConstat.NON_CONFORME)
                .filter(c -> {
                    String crit = c.getCriticite() != null ? c.getCriticite()
                        : (c.getControle() != null ? c.getControle().getCriticite() : null);
                    return "ELEVE".equalsIgnoreCase(crit) || "CRITIQUE".equalsIgnoreCase(crit);
                })
                .collect(Collectors.toList());

            if (critiques.isEmpty()) {
                addNormal(doc, "Aucune non-conformité critique identifiée.");
            } else {
                for (Constat c : critiques) {
                    String libelle = c.getControle() != null ? safe(c.getControle().getLibelle()) : "";
                    String crit = c.getCriticite() != null ? c.getCriticite()
                        : (c.getControle() != null ? safe(c.getControle().getCriticite()) : "");
                    addBullet(doc, "[" + crit + "] " + libelle);
                    if (c.getRecommandation() != null && !c.getRecommandation().isBlank()) {
                        addNormal(doc, "    ↳ Recommandation : " + c.getRecommandation());
                    }
                }
            }

            // ── Section 6 : Synthèse exécutive ─────────────────────────
            addHeading(doc, "6. Synthèse exécutive", 2);
            addNormal(doc, safe(synthese));

            // ── Section 7 : Actions correctives prioritaires ────────────
            addHeading(doc, "7. Actions correctives prioritaires", 2);
            List<Action> prioritaires = actions.stream()
                .filter(a -> a.getPriorite() == PrioriteAction.HAUTE || a.getPriorite() == PrioriteAction.CRITIQUE)
                .collect(Collectors.toList());

            if (prioritaires.isEmpty()) {
                addNormal(doc, "Aucune action corrective de haute priorité enregistrée.");
            } else {
                for (Action a : prioritaires) {
                    String ctrlLib = (a.getConstat() != null && a.getConstat().getControle() != null)
                        ? safe(a.getConstat().getControle().getLibelle()) : "";
                    String echeance = a.getEcheance() != null ? a.getEcheance().toString() : "N/A";
                    addBullet(doc, "[" + (a.getPriorite() != null ? a.getPriorite().name() : "?") + "] "
                        + ctrlLib + " — " + safe(a.getDescription()));
                    addNormal(doc, "    Responsable : " + safe(a.getResponsable())
                        + "  •  Échéance : " + echeance);
                }
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du document résumé", e);
            throw new RuntimeException("Erreur de génération du résumé", e);
        }
    }

    // ── POI helpers ────────────────────────────────────────────────────────────

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading" + level);
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(level == 1 ? 16 : 13);
        run.setText(text);
    }

    private void addBullet(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setNumID(null); // no list numbering, just indent
        XWPFRun run = p.createRun();
        run.setText("• " + text);
    }

    private void addNormal(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
    }
}
