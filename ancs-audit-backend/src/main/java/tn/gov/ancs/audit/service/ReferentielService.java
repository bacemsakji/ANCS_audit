package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Controle;
import tn.gov.ancs.audit.domain.Referentiel;
import tn.gov.ancs.audit.domain.enums.TypeReferentiel;
import tn.gov.ancs.audit.dto.response.ControleResponse;
import tn.gov.ancs.audit.dto.response.ReferentielResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.ControleRepository;
import tn.gov.ancs.audit.repository.ReferentielRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferentielService {

    private final ReferentielRepository referentielRepository;
    private final ControleRepository controleRepository;

    @Transactional(readOnly = true)
    public List<ReferentielResponse> getAllReferentiels(TypeReferentiel type) {
        List<Referentiel> list = (type != null)
            ? referentielRepository.findByType(type)
            : referentielRepository.findAll();
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReferentielResponse getReferentielById(UUID id) {
        Referentiel ref = referentielRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Référentiel non trouvé avec l'id: " + id));
        return mapToResponse(ref);
    }

    @Transactional(readOnly = true)
    public List<ControleResponse> getControlesByReferentielId(UUID referentielId) {
        if (!referentielRepository.existsById(referentielId)) {
            throw new ResourceNotFoundException("Référentiel non trouvé avec l'id: " + referentielId);
        }
        return controleRepository.findByReferentielIdOrderByCategorieAscOrdreAffichageAsc(referentielId).stream()
            .map(this::mapControleToResponse)
            .collect(Collectors.toList());
    }

    public ReferentielResponse mapToResponse(Referentiel ref) {
        return ReferentielResponse.builder()
            .id(ref.getId())
            .nom(ref.getNom())
            .type(ref.getType() != null ? ref.getType().name() : null)
            .version(ref.getVersion())
            .sourceUrl(ref.getSourceUrl())
            .description(ref.getDescription())
            .build();
    }

    public ControleResponse mapControleToResponse(Controle c) {
        return ControleResponse.builder()
            .id(c.getId())
            .referentielId(c.getReferentiel().getId())
            .libelle(c.getLibelle())
            .description(c.getDescription())
            .criticite(c.getCriticite())
            .categorie(c.getCategorie())
            .ordreAffichage(c.getOrdreAffichage())
            .build();
    }
}
