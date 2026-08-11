package tn.gov.ancs.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gov.ancs.audit.domain.Organisme;
import tn.gov.ancs.audit.domain.Utilisateur;
import tn.gov.ancs.audit.domain.enums.Role;
import tn.gov.ancs.audit.dto.request.CreateUserRequest;
import tn.gov.ancs.audit.dto.response.UtilisateurResponse;
import tn.gov.ancs.audit.exception.ResourceNotFoundException;
import tn.gov.ancs.audit.repository.OrganismeRepository;
import tn.gov.ancs.audit.repository.UtilisateurRepository;
import tn.gov.ancs.audit.security.AuditAction;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final OrganismeRepository organismeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @AuditAction(action = "CREATE_USER", resource = "UTILISATEUR")
    public UtilisateurResponse createUser(CreateUserRequest request) {
        if (utilisateurRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        Organisme organisme = null;
        if (request.getRole() == Role.RSSI) {
            if (request.getOrganismeId() == null) {
                throw new IllegalArgumentException("L'organisme est obligatoire pour le rôle RSSI");
            }
            organisme = organismeRepository.findById(request.getOrganismeId())
                .orElseThrow(() -> new ResourceNotFoundException("Organisme non trouvé avec l'id: " + request.getOrganismeId()));
        }

        Utilisateur user = Utilisateur.builder()
            .nom(request.getNom())
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .organisme(organisme)
            .isActive(true)
            .totpEnabled(false)
            .build();

        Utilisateur savedUser = utilisateurRepository.save(user);
        log.info("Nouvel utilisateur créé: {} avec le rôle {}", savedUser.getEmail(), savedUser.getRole());
        return mapToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getUserById(UUID id) {
        Utilisateur user = utilisateurRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getUserByEmail(String email) {
        Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email: " + email));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> getAllUsers() {
        return utilisateurRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(action = "TOGGLE_USER_STATUS", resource = "UTILISATEUR", extractResourceId = true)
    public UtilisateurResponse toggleUserStatus(UUID id) {
        Utilisateur user = utilisateurRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));
        
        if (user.getRole() == Role.ADMIN_ANCS && user.getIsActive()) {
            if (utilisateurRepository.countByRoleAndIsActiveTrue(Role.ADMIN_ANCS) <= 1) {
                throw new IllegalStateException("Impossible de désactiver le dernier administrateur actif");
            }
        }

        user.setIsActive(!user.getIsActive());
        Utilisateur updatedUser = utilisateurRepository.save(user);
        log.info("Statut de l'utilisateur {} modifié: actif={}", updatedUser.getEmail(), updatedUser.getIsActive());
        return mapToResponse(updatedUser);
    }

    public UtilisateurResponse mapToResponse(Utilisateur user) {
        return UtilisateurResponse.builder()
            .id(user.getId())
            .nom(user.getNom())
            .email(user.getEmail())
            .role(user.getRole().name())
            .organismeId(user.getOrganisme() != null ? user.getOrganisme().getId() : null)
            .organismeNom(user.getOrganisme() != null ? user.getOrganisme().getNom() : null)
            .totpEnabled(user.getTotpEnabled())
            .isActive(user.getIsActive())
            .build();
    }
}
