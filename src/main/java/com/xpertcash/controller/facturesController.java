package com.xpertcash.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.service.AuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class facturesController {

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    /** Factures de l'entreprise, paginées en base (isolation multi-tenant). */
    @GetMapping("/factures")
    public ResponseEntity<?> getFacturesPaginated(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateFacture") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Aucune entreprise associée à cet utilisateur"));
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Facture> facturePage = factureRepository.findAllByEntrepriseIdPaginated(entreprise.getId(), pageable);
        List<FactureDTO> content = facturePage.getContent().stream().map(FactureDTO::new).collect(Collectors.toList());
        Page<FactureDTO> dtoPage = new PageImpl<>(content, facturePage.getPageable(), facturePage.getTotalElements());
        PaginatedResponseDTO<FactureDTO> result = PaginatedResponseDTO.fromPage(dtoPage);
        return ResponseEntity.ok(result);
    }

    /** Factures d'une boutique de l'entreprise, paginées en base (isolation multi-tenant). */
    @GetMapping("/factures/{boutiqueId}")
    public ResponseEntity<?> getFacturesByBoutiquePaginated(
            @PathVariable Long boutiqueId,
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateFacture") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Aucune entreprise associée à cet utilisateur"));
        }

        boutiqueRepository.findByIdAndEntrepriseId(boutiqueId, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Boutique introuvable ou n'appartient pas à votre entreprise"));

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Facture> facturePage = factureRepository.findByBoutiqueIdAndEntrepriseIdPaginated(boutiqueId, entreprise.getId(), pageable);
        List<FactureDTO> content = facturePage.getContent().stream().map(FactureDTO::new).collect(Collectors.toList());
        Page<FactureDTO> dtoPage = new PageImpl<>(content, facturePage.getPageable(), facturePage.getTotalElements());
        PaginatedResponseDTO<FactureDTO> result = PaginatedResponseDTO.fromPage(dtoPage);
        return ResponseEntity.ok(result);
    }
}
