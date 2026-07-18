package tn.gov.ancs.audit.dto.response;

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
public class ReunionTravailResponse {

    private UUID id;
    private UUID missionId;
    private Instant dateReunion;
    private String participants;
    private String compteRendu;
}
