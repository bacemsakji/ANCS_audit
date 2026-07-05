package tn.gov.ancs.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferentielResponse {

    private UUID id;
    private String nom;
    private String type;
    private String version;
    private String sourceUrl;
    private String description;
}
