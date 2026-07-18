package tn.gov.ancs.audit.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.gov.ancs.audit.domain.Action;
import tn.gov.ancs.audit.domain.enums.StatutAction;
import tn.gov.ancs.audit.repository.ActionRepository;
import tn.gov.ancs.audit.service.NotificationService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Planificateur de notifications pour les actions correctives en retard.
 *
 * <p>Vérifie quotidiennement les actions dont l'échéance est dépassée ou imminente
 * et envoie des notifications push FCM aux responsables.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ActionRepository actionRepository;
    private final NotificationService notificationService;

    /**
     * Vérifie les actions en retard (échéance dépassée) et envoie des notifications.
     * Exécuté tous les jours à 8h00.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkOverdueActions() {
        log.info("Vérification des actions correctives en retard...");
        
        LocalDate today = LocalDate.now();
        List<Action> overdueActions = actionRepository.findEnRetard(today);

        log.info("{} actions en retard détectées", overdueActions.size());

        for (Action action : overdueActions) {
            try {
                // Mettre à jour le statut en EN_RETARD
                action.setStatut(StatutAction.EN_RETARD);
                actionRepository.save(action);
                
                // Envoyer notification au RSSI de l'organisme
                UUID organismeId = action.getConstat().getMission().getOrganisme().getId();
                sendNotificationToRssi(organismeId, action, "ACTION_OVERDUE", 
                    "Action corrective en retard",
                    String.format("L'action \"%s\" (échéance: %s) est en retard de clôture.",
                        action.getDescription(), action.getEcheance()));
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de notification pour l'action {}", action.getId(), e);
            }
        }
    }

    /**
     * Vérifie les actions dont l'échéance est dans 3 jours (rappel).
     * Exécuté tous les jours à 9h00.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkUpcomingDeadlines() {
        log.info("Vérification des échéances imminentes...");
        
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);
        List<Action> upcomingActions = actionRepository.findEcheancesProches(LocalDate.now(), threeDaysLater);

        log.info("{} actions avec échéance imminente détectées", upcomingActions.size());

        for (Action action : upcomingActions) {
            try {
                UUID organismeId = action.getConstat().getMission().getOrganisme().getId();
                sendNotificationToRssi(organismeId, action, "ACTION_REMINDER",
                    "Rappel : Échéance action corrective",
                    String.format("L'action \"%s\" arrive à échéance dans 3 jours (%s).",
                        action.getDescription(), action.getEcheance()));
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de rappel pour l'action {}", action.getId(), e);
            }
        }
    }

    /**
     * Envoie une notification à tous les utilisateurs RSSI d'un organisme.
     */
    private void sendNotificationToRssi(UUID organismeId, Action action, String type, String title, String body) {
        // Note: Pour l'implémentation complète, il faudrait récupérer les utilisateurs RSSI
        // de l'organisme via UtilisateurRepository. Pour l'instant, on loggue l'action.
        log.info("Notification {} pour organisme {}: {} - {}", type, organismeId, title, body);
        
        Map<String, String> extraData = new HashMap<>();
        extraData.put("actionId", action.getId().toString());
        extraData.put("type", type);
        extraData.put("organismeId", organismeId.toString());
        
        // TODO: Récupérer les utilisateurs RSSI de l'organisme et envoyer les notifications
        // notificationService.sendPushToUser(userId, title, body, extraData);
    }
}
