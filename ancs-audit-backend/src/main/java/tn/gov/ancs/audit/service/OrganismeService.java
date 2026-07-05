package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Organisme;
import tn.gov.ancs.audit.dto.request.OrganismeRequest;
import tn.gov.ancs.audit.dto.response.OrganismeResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.OrganismeRepository;
import tn.gov.ancs.audit.security.AuditAction;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganismeService {

    private final OrganismeRepository organismeRepository;

    @Transactional
    @AuditAction(action = "CREATE_ORGANISME", resource = "ORGANISME")
    public OrganismeResponse createOrganisme(OrganismeRequest request) {
        if (organismeRepository.existsByNomIgnoreCase(request.getNom())) {
            throw new IllegalArgumentException("Un organisme avec ce nom existe déjà");
        }

        Organisme organisme = Organisme.builder()
            .nom(request.getNom())
            .secteurActivite(request.getSecteurActivite())
            .typeObligationAudit(request.getTypeObligationAudit())
            .adresse(request.getAdresse())
            .contactRssiEmail(request.getContactRssiEmail())
            .build();

        Organisme saved = organismeRepository.save(organisme);
        log.info("Nouvel organisme créé : {}", saved.getNom());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganismeResponse getOrganismeById(UUID id) {
        Organisme org = organismeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Organisme non trouvé avec l'id: " + id));
        return mapToResponse(org);
    }

    @Transactional(readOnly = true)
    public Page<OrganismeResponse> getOrganismes(String secteurActivite, Pageable pageable) {
        Page<Organisme> list = (secteurActivite != null && !secteurActivite.isBlank())
            ? organismeRepository.findBySecteurActivite(secteurActivite, pageable)
            : organismeRepository.findAll(pageable);
        return list.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<OrganismeResponse> getAllOrganismesList() {
        return organismeRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private OrganismeResponse mapToResponse(Organisme org) {
        return OrganismeResponse.builder()
            .id(org.getId())
            .nom(org.getNom())
            .secteurActivite(org.getSecteurActivite())
            .typeObligationAudit(org.getTypeObligationAudit())
            .adresse(org.getAdresse())
            .contactRssiEmail(org.getContactRssiEmail())
            .build();
    }
}
