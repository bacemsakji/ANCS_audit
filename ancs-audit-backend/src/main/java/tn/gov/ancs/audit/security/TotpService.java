package tn.gov.ancs.audit.security;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {

    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;

    /**
     * Génère un secret TOTP cryptographiquement sûr encodé en Base32.
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Génère l'URI de données (Data URI PNG) contenant le QR code d'enrôlement 2FA.
     */
    public String getQrCodeImageUri(String email, String secret) {
        QrData data = qrDataFactory.newBuilder()
            .label(email)
            .secret(secret)
            .issuer("ANCS Audit Tunisie")
            .build();

        try {
            byte[] imageData = qrGenerator.generate(data);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Erreur lors de la génération du QR Code 2FA pour {}", email, e);
            throw new RuntimeException("Erreur de génération du QR Code 2FA", e);
        }
    }

    /**
     * Vérifie la validité d'un code TOTP fourni par rapport au secret de l'utilisateur.
     */
    public boolean verifyCode(String code, String secret) {
        if (code == null || secret == null) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code.trim());
    }
}
