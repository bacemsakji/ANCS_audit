package tn.gov.ancs.audit.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.repository.UtilisateurRepository;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UtilisateurRepository utilisateurRepository;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    /**
     * Envoie une notification push FCM à un utilisateur spécifique si son token est enregistré.
     */
    public void sendPushToUser(UUID userId, String title, String body, Map<String, String> extraData) {
        utilisateurRepository.findById(userId).ifPresentOrElse(
            user -> {
                String token = user.getFcmToken();
                if (token != null && !token.isBlank()) {
                    sendNotification(token, title, body, extraData);
                } else {
                    log.warn("Impossible d'envoyer la notification push : aucun token FCM trouvé pour l'utilisateur {}", user.getEmail());
                }
            },
            () -> log.warn("Utilisateur non trouvé avec l'id {} pour l'envoi de notification", userId)
        );
    }

    private void sendNotification(String token, String title, String body, Map<String, String> extraData) {
        if (!firebaseEnabled) {
            log.info("[NOTIFICATION LOG] (Firebase désactivé) destinataire token: {}, titre: '{}', message: '{}'", token, title, body);
            return;
        }

        try {
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification);

            if (extraData != null && !extraData.isEmpty()) {
                messageBuilder.putAllData(extraData);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Notification push FCM envoyée avec succès, id message: {}", response);

        } catch (Exception e) {
            log.error("Échec de l'envoi de la notification push FCM à la cible", e);
        }
    }
}
