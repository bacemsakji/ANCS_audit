package tn.gov.ancs.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditeurResponse {

    private UUID id;
    private UUID utilisateurId;
    private String nom;
    private String email;
    private String numeroCertification;
    private LocalDate dateCertification;
    private LocalDate dateExpiration;
    private List<String> specialites;
    private String statut;
}
