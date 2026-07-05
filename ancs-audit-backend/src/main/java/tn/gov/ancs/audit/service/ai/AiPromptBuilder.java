package tn.gov.ancs.audit.service.ai;

import org.springframework.stereotype.Component;
import tn.gov.ancs.audit.dto.request.AiSummaryRequest;

@Component
public class AiPromptBuilder {

    /**
     * Construit le prompt système pour cadrer le ton institutionnel.
     */
    public String buildSystemPrompt(String langue) {
        if ("AR".equalsIgnoreCase(langue)) {
            return """
                أنت مساعد تحرير تقارير تدقيق الأمن السيبراني معتمد لدى الوكالة الوطنية للسلامة السيبرانية (ANCS) في تونس.
                اكتب ملخصًا تنفيذيًا مهنيًا ومؤسسيًا للتقرير.
                الأسلوب: رسمي، رصين، وموضوعي تمامًا. الطول: بين 200 إلى 300 كلمة.
                الهيكل الإلزامي:
                1) السياق والمهام (الجهة المدققة، التواريخ، الإطار التنظيمي).
                2) منهجية التدقيق المعتمدة.
                3) النتائج الرئيسية ومعدل الامتثال العام.
                4) التوصيات العامة.
                """;
        }
        
        return """
            Tu es un assistant de rédaction pour un auditeur certifié de l'Agence Nationale de Cybersécurité (ANCS) en Tunisie.
            Rédige une synthèse exécutive hautement professionnelle et institutionnelle.
            Le ton doit être formel, sobre, purement factuel et constructif. Longueur limitée à 200-300 mots.
            Structure imposée à respecter scrupuleusement :
            1) Contexte et périmètre (nom de l'organisme, dates de la mission, cadre réglementaire).
            2) Méthodologie résumée.
            3) Principaux constats et indicateur du taux de conformité global.
            4) Recommandation stratégique générale.
            """;
    }

    /**
     * Construit le prompt utilisateur à partir des données de la mission et des constats.
     */
    public String buildUserPrompt(AiSummaryRequest request) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Données de la mission d'audit :\n");
        sb.append("- Organisme audité : ").append(request.getOrganismeNom()).append("\n");
        sb.append("- Périmètre : ").append(request.getPerimetre() != null ? request.getPerimetre() : "Non spécifié").append("\n");
        sb.append("- Période : du ").append(request.getDateDebut() != null ? request.getDateDebut() : "N/A");
        sb.append(" au ").append(request.getDateFin() != null ? request.getDateFin() : "N/A").append("\n");
        sb.append("- Référentiel réglementaire : ").append(request.getReferentielNom());
        sb.append(" v").append(request.getReferentielVersion() != null ? request.getReferentielVersion() : "1.0").append("\n");
        sb.append("- Taux de conformité global calculé : ").append(String.format("%.1f", request.getTauxConformite())).append("%\n\n");
        
        sb.append("Liste des constats d'audit significatifs à analyser :\n");
        if (request.getConstats() == null || request.getConstats().isEmpty()) {
            sb.append("- Aucun constat significatif enregistré. Tout est conforme.");
        } else {
            for (AiSummaryRequest.ConstatInfo c : request.getConstats()) {
                sb.append(String.format("- [%s] %s : %s", 
                    c.getCriticite() != null ? c.getCriticite() : "MOYEN",
                    c.getControleLibelle(), 
                    c.getResultat()));
                if (c.getCommentaire() != null && !c.getCommentaire().isBlank()) {
                    sb.append(" (Observation : ").append(c.getCommentaire()).append(")");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
}
