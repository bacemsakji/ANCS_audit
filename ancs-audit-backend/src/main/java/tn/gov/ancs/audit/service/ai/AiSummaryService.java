package tn.gov.ancs.audit.service.ai;

import tn.gov.ancs.audit.dto.request.AiSummaryRequest;
import tn.gov.ancs.audit.exception.AiUnavailableException;

public interface AiSummaryService {

    /**
     * Génère une proposition de synthèse exécutive.
     * Cette méthode ne modifie pas l'état en base de données.
     *
     * @param request Contexte complet de la mission et des constats.
     * @return Proposition de texte rédigée par l'IA.
     * @throws AiUnavailableException si le service LLM configuré est injoignable ou en échec.
     */
    String generateDraft(AiSummaryRequest request) throws AiUnavailableException;
}
