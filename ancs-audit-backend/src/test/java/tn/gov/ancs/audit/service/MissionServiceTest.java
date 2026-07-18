package tn.gov.ancs.audit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.gov.ancs.audit.domain.Auditeur;
import tn.gov.ancs.audit.domain.Organisme;
import tn.gov.ancs.audit.domain.Referentiel;
import tn.gov.ancs.audit.domain.enums.TypeReferentiel;
import tn.gov.ancs.audit.dto.request.CreateMissionRequest;
import tn.gov.ancs.audit.repository.AuditeurRepository;
import tn.gov.ancs.audit.repository.MissionRepository;
import tn.gov.ancs.audit.repository.OrganismeRepository;
import tn.gov.ancs.audit.repository.ReferentielRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private OrganismeRepository organismeRepository;

    @Mock
    private AuditeurRepository auditeurRepository;

    @Mock
    private ReferentielRepository referentielRepository;

    @InjectMocks
    private MissionService missionService;

    @Test
    void createMission_WithInvalidReferentielType_ShouldThrowIllegalArgumentException() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UUID audId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        CreateMissionRequest request = CreateMissionRequest.builder()
            .organismeId(orgId)
            .auditeurId(audId)
            .referentielId(refId)
            .build();

        Organisme organisme = new Organisme();
        Auditeur auditeur = new Auditeur();
        Referentiel referentiel = Referentiel.builder()
            .nom("Décret-loi n° 2023-17")
            .type(TypeReferentiel.LOI)
            .build();

        when(organismeRepository.findById(orgId)).thenReturn(Optional.of(organisme));
        when(auditeurRepository.findById(audId)).thenReturn(Optional.of(auditeur));
        when(referentielRepository.findById(refId)).thenReturn(Optional.of(referentiel));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            missionService.createMission(request);
        });

        assertEquals("Le référentiel sélectionné doit être de type CONTROLE_TECHNIQUE", exception.getMessage());
        verify(missionRepository, never()).save(any());
    }
}
