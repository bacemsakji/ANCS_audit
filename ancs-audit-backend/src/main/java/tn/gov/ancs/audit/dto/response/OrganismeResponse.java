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
public class OrganismeResponse {

    private UUID id;
    private String nom;
    private String secteurActivite;
    private String typeObligationAudit;
    private String adresse;
    private String contactRssiEmail;
    private String acronyme;
    private String statut;
    private String categorie;
}
