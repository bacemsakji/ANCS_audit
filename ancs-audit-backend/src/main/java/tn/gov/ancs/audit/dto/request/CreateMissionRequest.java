package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMissionRequest {

    @NotNull(message = "L'organisme à auditer est obligatoire")
    private UUID organismeId;

    @NotNull(message = "L'auditeur assigné est obligatoire")
    private UUID auditeurId;

    @NotNull(message = "Le référentiel applicable est obligatoire")
    private UUID referentielId;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String perimetre;
}
