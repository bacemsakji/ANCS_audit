package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateAuditeurRequest {

    @NotNull(message = "L'utilisateur associé est obligatoire")
    private UUID utilisateurId;

    @NotBlank(message = "Le numéro de certification est obligatoire")
    @Size(max = 100)
    private String numeroCertification;

    @NotNull(message = "La date de certification est obligatoire")
    private LocalDate dateCertification;

    @NotNull(message = "La date d'expiration est obligatoire")
    private LocalDate dateExpiration;

    private List<String> specialites;
}
