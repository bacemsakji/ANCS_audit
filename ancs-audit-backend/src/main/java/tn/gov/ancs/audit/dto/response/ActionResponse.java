package tn.gov.ancs.audit.dto.response;

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
public class ActionResponse {

    private UUID id;
    private UUID constatId;
    private String constatControleLibelle;
    private String description;
    private String responsable;
    private LocalDate echeance;
    private String priorite;
    private String statut;
}
