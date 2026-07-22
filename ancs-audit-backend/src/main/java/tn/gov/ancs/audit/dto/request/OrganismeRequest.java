package tn.gov.ancs.audit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganismeRequest {

    @NotBlank(message = "Le nom de l'organisme est obligatoire")
    @Size(max = 255)
    private String nom;

    @Size(max = 100)
    private String secteurActivite;

    @Size(max = 50)
    private String typeObligationAudit;

    private String adresse;

    @Email(message = "L'adresse e-mail doit être valide")
    @Size(max = 255)
    private String contactRssiEmail;

    @Size(max = 50)
    private String acronyme;

    @Size(max = 20)
    private String statut;

    @Size(max = 100)
    private String categorie;
}
