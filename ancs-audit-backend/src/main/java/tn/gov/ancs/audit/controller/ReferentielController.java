package tn.gov.ancs.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.gov.ancs.audit.domain.enums.TypeReferentiel;
import tn.gov.ancs.audit.dto.response.ControleResponse;
import tn.gov.ancs.audit.dto.response.ReferentielResponse;
import tn.gov.ancs.audit.service.ReferentielService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/referentiels")
@RequiredArgsConstructor
public class ReferentielController {

    private final ReferentielService referentielService;

    @GetMapping
    public ResponseEntity<List<ReferentielResponse>> getAllReferentiels(
            @RequestParam(value = "type", required = false) TypeReferentiel type) {
        List<ReferentielResponse> response = referentielService.getAllReferentiels(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferentielResponse> getReferentielById(@PathVariable("id") UUID id) {
        ReferentielResponse response = referentielService.getReferentielById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/controles")
    public ResponseEntity<List<ControleResponse>> getControles(@PathVariable("id") UUID id) {
        List<ControleResponse> response = referentielService.getControlesByReferentielId(id);
        return ResponseEntity.ok(response);
    }
}
