package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.gov.ancs.audit.domain.enums.PrioriteAction;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequest {

    @NotNull(message = "L'identifiant du constat associé est obligatoire")
    private UUID constatId;

    @NotBlank(message = "La description de l'action corrective est obligatoire")
    private String description;

    private String responsable;
    private LocalDate echeance;

    @NotNull(message = "La priorité est obligatoire")
    private PrioriteAction priorite;
}
