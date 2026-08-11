package tn.gov.ancs.audit.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.buckets.rapports}")
    private String rapportsBucket;

    @Value("${minio.buckets.preuves}")
    private String preuvesBucket;

    @Value("${minio.signed-url-expiry-hours}")
    private int signedUrlExpiryHours;

    private static final java.util.List<String> ALLOWED_MIME_TYPES = java.util.List.of(
        "image/png", "image/jpeg", "image/gif", "application/pdf",
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final java.util.List<String> ALLOWED_EXTENSIONS = java.util.List.of(
        "png", "jpg", "jpeg", "gif", "pdf", "doc", "docx"
    );

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Type MIME non autorisé : " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Nom de fichier obligatoire");
        }

        int lastDot = originalFilename.lastIndexOf(".");
        if (lastDot == -1) {
            throw new IllegalArgumentException("Le fichier doit avoir une extension valide");
        }

        String ext = originalFilename.substring(lastDot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Extension non autorisée : ." + ext);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }
        // Extraire uniquement le nom du fichier de base (sans chemins d'accès)
        String baseName = new java.io.File(filename).getName();
        
        // Conserver uniquement les caractères alphanumériques et standards
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Empêcher les préfixes dangereux ou un nom vide
        if (baseName.startsWith("..") || baseName.isBlank()) {
            baseName = "safe_" + System.currentTimeMillis() + "_" + baseName;
        }
        return baseName;
    }

    /**
     * Téléverse un fichier (ex. preuve ou constat) dans le bucket des preuves.
     * Retourne l'identifiant/chemin unique du fichier stocké.
     */
    public String uploadPreuve(MultipartFile file) {
        // SÉCURITÉ : valider l'extension et le type MIME (bloque SVG, HTML et scripts d'attaque XSS)
        validateFile(file);
        
        // SÉCURITÉ : assainir le nom de fichier contre le path traversal
        String safeName = sanitizeFilename(file.getOriginalFilename());
        String objectName = UUID.randomUUID() + "_" + safeName;
        
        return uploadFile(preuvesBucket, objectName, file);
    }

    /**
     * Téléverse un flux d'entrée (ex. rapport généré) dans le bucket des rapports.
     */
    public String uploadRapport(String filename, byte[] fileBytes, String contentType) {
        String safeName = sanitizeFilename(filename);
        String objectName = UUID.randomUUID() + "_" + safeName;
        try (InputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(rapportsBucket)
                    .object(objectName)
                    .stream(inputStream, fileBytes.length, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("Rapport téléversé avec succès dans MinIO: {}/{}", rapportsBucket, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Échec du téléversement du rapport dans MinIO", e);
            throw new RuntimeException("Erreur de stockage du rapport", e);
        }
    }

    /**
     * Génère une URL de téléchargement pré-signée et temporaire (sécurisée) pour un fichier.
     */
    public String getPresignedUrl(String bucketName, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(signedUrlExpiryHours, TimeUnit.HOURS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Échec de la génération de l'URL pré-signée pour {}/{}", bucketName, objectName, e);
            throw new RuntimeException("Erreur lors de la récupération du lien sécurisé", e);
        }
    }

    public String getPreuvesBucket() {
        return preuvesBucket;
    }

    public String getRapportsBucket() {
        return rapportsBucket;
    }

    private String uploadFile(String bucket, String objectName, MultipartFile file) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            log.info("Fichier téléversé avec succès dans MinIO: {}/{}", bucket, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Échec du téléversement du fichier dans MinIO", e);
            throw new RuntimeException("Erreur lors du stockage du fichier", e);
        }
    }
}
