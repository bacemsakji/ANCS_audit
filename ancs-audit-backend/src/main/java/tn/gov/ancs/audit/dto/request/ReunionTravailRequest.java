package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReunionTravailRequest {

    @NotNull(message = "L'identifiant de la mission est obligatoire")
    private UUID missionId;

    @NotNull(message = "La date de la réunion est obligatoire")
    private Instant dateReunion;

    private String participants;

    private String compteRendu;
}
