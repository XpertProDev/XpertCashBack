package com.xpertcash.service.VENTE;

import com.xpertcash.DTOs.VENTE.FactureDepensePaginatedDTO;
import com.xpertcash.entity.*;
import com.xpertcash.entity.VENTE.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.*;
import java.util.Optional;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.PermissionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FactureDepenseService {

    @Autowired
    private AuthenticationHelper authHelper;
    
    @Autowired
    private VenteRepository venteRepository;
    
    @Autowired
    private FactureVenteRepository factureVenteRepository;
    
    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;
    
    @Autowired
    private CaisseRepository caisseRepository;
    

    /**
     * Récupère toutes les factures de vente et dépenses avec pagination
     * pour l'entreprise de l'utilisateur connecté
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getAllFacturesEtDepenses(
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            String typeFilter, 
            HttpServletRequest request) {
        
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        // Configuration du tri (la pagination sera appliquée manuellement)
        // Sort sort = sortDir.equalsIgnoreCase("desc") ? 
        //     Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        // Si pas de dates spécifiées, prendre l'année en cours
        if (dateDebut == null) {
            LocalDate today = LocalDate.now();
            dateDebut = today.withDayOfYear(1).atStartOfDay();
        }
        if (dateFin == null) {
            dateFin = LocalDateTime.now();
        }
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems = new ArrayList<>();
        
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("FACTURE_VENTE")) {
            List<Vente> ventes;
            
            if (!isAdminOrManager) {
                ventes = venteRepository.findByVendeur_IdAndDateVenteBetween(user.getId(), dateDebut, dateFin);
            } else {
                ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                    entrepriseId, dateDebut, dateFin);
            }
            
            for (Vente vente : ventes) {
                FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                item.setId(vente.getId());
                item.setType("FACTURE_VENTE");
                item.setDescription("Vente - " + (vente.getClientNom() != null ? vente.getClientNom() : "Client passant"));
                item.setMontant(vente.getMontantTotal());
                item.setDate(vente.getDateVente());
                item.setBoutique(vente.getBoutique().getNomBoutique());
                item.setVendeur(vente.getVendeur().getNomComplet());
                item.setCaisseId(vente.getCaisse().getId());
                item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
                item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
                item.setClientNom(vente.getClientNom());
                item.setClientNumero(vente.getClientNumero());
                item.setRemiseGlobale(vente.getRemiseGlobale());
                
                Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                        ? vente.getBoutique().getEntreprise().getId() : null;
                Optional<FactureVente> factureVente = venteEntrepriseId != null 
                        ? factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                        : Optional.empty();
                if (factureVente.isPresent()) {
                    item.setNumeroFactureVente(factureVente.get().getNumeroFacture());
                }
                
                if (vente.getProduits() != null) {
                    List<FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO> produitsDTO = 
                        vente.getProduits().stream().map(vp -> {
                            FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO produitDTO = 
                                new FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO();
                            produitDTO.setProduitId(vp.getProduit().getId());
                            produitDTO.setNomProduit(vp.getProduit().getNom());
                            if (vp.isEstRemboursee() && vp.getQuantiteRemboursee() != null && vp.getQuantiteRemboursee() > 0) {
                                produitDTO.setQuantite(vp.getQuantiteRemboursee());
                            } else {
                                produitDTO.setQuantite(vp.getQuantite());
                            }
                            produitDTO.setPrixUnitaire(vp.getPrixUnitaire());
                            produitDTO.setMontantLigne(vp.getMontantLigne());
                            produitDTO.setRemise(vp.getRemise());
                            return produitDTO;
                        }).collect(Collectors.toList());
                    item.setProduits(produitsDTO);
                }
                
                allItems.add(item);
            }
        }
        
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("DEPENSE")) {
            List<MouvementCaisse> depenses;
            
            if (!isAdminOrManager) {
                List<Caisse> userCaisses = caisseRepository.findByVendeur_Id(user.getId());
                List<Long> caisseIds = userCaisses.stream().map(Caisse::getId).collect(Collectors.toList());
                
                if (!caisseIds.isEmpty()) {
                    depenses = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvementAndDateMouvementBetween(
                        caisseIds, TypeMouvementCaisse.DEPENSE, dateDebut, dateFin);
                } else {
                    depenses = new ArrayList<>();
                }
            } else {
                depenses = mouvementCaisseRepository.findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                    entrepriseId, TypeMouvementCaisse.DEPENSE, dateDebut, dateFin);
            }
            
            for (MouvementCaisse depense : depenses) {
                FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                item.setId(depense.getId());
                item.setType("DEPENSE");
                item.setDescription(depense.getDescription());
                item.setMontant(depense.getMontant());
                item.setDate(depense.getDateMouvement());
                item.setBoutique(depense.getCaisse().getBoutique().getNomBoutique());
                item.setVendeur(depense.getCaisse().getVendeur().getNomComplet());
                item.setCaisseId(depense.getCaisse().getId());
                item.setModePaiement(depense.getModePaiement() != null ? depense.getModePaiement().name() : null);
                item.setStatut("EFFECTUE");
                item.setMotifDepense(depense.getDescription());
                item.setTypeMouvement(depense.getTypeMouvement().name());
                
                allItems.add(item);
            }
        }
        
        if (typeFilter == null || typeFilter.equals("ALL")) {
            List<TypeMouvementCaisse> autresTypes = Arrays.asList(
                TypeMouvementCaisse.AJOUT, 
                TypeMouvementCaisse.RETRAIT, 
                TypeMouvementCaisse.REMBOURSEMENT
            );
            
            for (TypeMouvementCaisse type : autresTypes) {
                List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                    entrepriseId, type, dateDebut, dateFin);
                
                for (MouvementCaisse mouvement : mouvements) {
                    FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                    item.setId(mouvement.getId());
                    item.setType("MOUVEMENT_CAISSE");
                    item.setDescription(mouvement.getDescription());
                    item.setMontant(mouvement.getMontant());
                    item.setDate(mouvement.getDateMouvement());
                    item.setBoutique(mouvement.getCaisse().getBoutique().getNomBoutique());
                    item.setVendeur(mouvement.getCaisse().getVendeur().getNomComplet());
                    item.setCaisseId(mouvement.getCaisse().getId());
                    item.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
                    item.setStatut("EFFECTUE");
                    item.setTypeMouvement(mouvement.getTypeMouvement().name());
                    
                    allItems.add(item);
                }
            }
        }
        
        allItems.sort((item1, item2) -> {
            if (sortDir.equalsIgnoreCase("desc")) {
                return item2.getDate().compareTo(item1.getDate());
            } else {
                return item1.getDate().compareTo(item2.getDate());
            }
        });
        
        double totalFacturesVente = allItems.stream()
            .filter(item -> "FACTURE_VENTE".equals(item.getType()))
            .mapToDouble(item -> item.getMontant() != null ? item.getMontant() : 0.0)
            .sum();
        
        double totalDepenses = allItems.stream()
            .filter(item -> "DEPENSE".equals(item.getType()) || "MOUVEMENT_CAISSE".equals(item.getType()))
            .mapToDouble(item -> item.getMontant() != null ? item.getMontant() : 0.0)
            .sum();
        
        int nombreFacturesVente = (int) allItems.stream()
            .filter(item -> "FACTURE_VENTE".equals(item.getType()))
            .count();
        
        int nombreDepenses = (int) allItems.stream()
            .filter(item -> "DEPENSE".equals(item.getType()) || "MOUVEMENT_CAISSE".equals(item.getType()))
            .count();
        
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> pagedItems = 
            startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
        
        FactureDepensePaginatedDTO response = new FactureDepensePaginatedDTO();
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1);
        response.setHasNext(page < totalPages - 1);
        response.setHasPrevious(page > 0);
        response.setTotalFacturesVente(totalFacturesVente);
        response.setTotalDepenses(totalDepenses);
        response.setSoldeNet(totalFacturesVente - totalDepenses);
        response.setNombreFacturesVente(nombreFacturesVente);
        response.setNombreDepenses(nombreDepenses);
        response.setItems(pagedItems);
        
        return response;
    }

    /**
     * Récupère les factures et dépenses du jour avec pagination
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getFacturesEtDepensesDuJour(
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            String typeFilter,
            HttpServletRequest request) {
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        return getAllFacturesEtDepenses(page, size, sortBy, sortDir, startOfDay, endOfDay, typeFilter, request);
    }

    /**
     * Récupère les factures et dépenses du mois avec pagination
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getFacturesEtDepensesDuMois(
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            String typeFilter,
            HttpServletRequest request) {
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        
        return getAllFacturesEtDepenses(page, size, sortBy, sortDir, startOfMonth, endOfMonth, typeFilter, request);
    }

    /**
     * Récupère les factures et dépenses pour une boutique spécifique
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getFacturesEtDepensesByBoutique(
            Long boutiqueId,
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            String typeFilter,
            HttpServletRequest request) {
        
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        if (!isAdminOrManager) {
            boolean userBelongsToBoutique = user.getUserBoutiques().stream()
                .anyMatch(ub -> ub.getBoutique().getId().equals(boutiqueId));
            if (!userBelongsToBoutique) {
                throw new RuntimeException("Vous ne pouvez accéder qu'aux données de votre boutique.");
            }
        }
        
        // Si pas de dates spécifiées, prendre l'année en cours
        if (dateDebut == null) {
            LocalDate today = LocalDate.now();
            dateDebut = today.withDayOfYear(1).atStartOfDay();
        }
        if (dateFin == null) {
            dateFin = LocalDateTime.now();
        }
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems = new ArrayList<>();
        
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("FACTURE_VENTE")) {
            List<Vente> ventes = venteRepository.findByBoutiqueIdAndDateRange(boutiqueId, dateDebut, dateFin);
            
            for (Vente vente : ventes) {
                if (!vente.getBoutique().getEntreprise().getId().equals(entrepriseId)) {
                    continue;
                }
                
                FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                item.setId(vente.getId());
                item.setType("FACTURE_VENTE");
                item.setDescription("Vente - " + (vente.getClientNom() != null ? vente.getClientNom() : "Client passant"));
                item.setMontant(vente.getMontantTotal());
                item.setDate(vente.getDateVente());
                item.setBoutique(vente.getBoutique().getNomBoutique());
                item.setVendeur(vente.getVendeur().getNomComplet());
                item.setCaisseId(vente.getCaisse().getId());
                item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
                item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
                item.setClientNom(vente.getClientNom());
                item.setClientNumero(vente.getClientNumero());
                item.setRemiseGlobale(vente.getRemiseGlobale());
                
                Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                        ? vente.getBoutique().getEntreprise().getId() : null;
                Optional<FactureVente> factureVente = venteEntrepriseId != null 
                        ? factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                        : Optional.empty();
                if (factureVente.isPresent()) {
                    item.setNumeroFactureVente(factureVente.get().getNumeroFacture());
                }
                
                if (vente.getProduits() != null) {
                    List<FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO> produitsDTO = 
                        vente.getProduits().stream().map(vp -> {
                            FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO produitDTO = 
                                new FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO();
                            produitDTO.setProduitId(vp.getProduit().getId());
                            produitDTO.setNomProduit(vp.getProduit().getNom());
                            if (vp.isEstRemboursee() && vp.getQuantiteRemboursee() != null && vp.getQuantiteRemboursee() > 0) {
                                produitDTO.setQuantite(vp.getQuantiteRemboursee());
                            } else {
                                produitDTO.setQuantite(vp.getQuantite());
                            }
                            produitDTO.setPrixUnitaire(vp.getPrixUnitaire());
                            produitDTO.setMontantLigne(vp.getMontantLigne());
                            produitDTO.setRemise(vp.getRemise());
                            return produitDTO;
                        }).collect(Collectors.toList());
                    item.setProduits(produitsDTO);
                }
                
                allItems.add(item);
            }
        }
        
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("DEPENSE")) {
            List<Caisse> caisses = caisseRepository.findByBoutiqueId(boutiqueId);
            
            for (Caisse caisse : caisses) {
                if (!caisse.getBoutique().getEntreprise().getId().equals(entrepriseId)) {
                    continue;
                }
                
                List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                    caisse.getId(), TypeMouvementCaisse.DEPENSE);
                
                for (MouvementCaisse mouvement : mouvements) {
                    if (mouvement.getDateMouvement().isAfter(dateDebut) && 
                        mouvement.getDateMouvement().isBefore(dateFin)) {
                        
                        FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                        item.setId(mouvement.getId());
                        item.setType("DEPENSE");
                        item.setDescription(mouvement.getDescription());
                        item.setMontant(mouvement.getMontant());
                        item.setDate(mouvement.getDateMouvement());
                        item.setBoutique(mouvement.getCaisse().getBoutique().getNomBoutique());
                        item.setVendeur(mouvement.getCaisse().getVendeur().getNomComplet());
                        item.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
                        item.setStatut("EFFECTUE");
                        item.setMotifDepense(mouvement.getDescription());
                        item.setTypeMouvement(mouvement.getTypeMouvement().name());
                        
                        allItems.add(item);
                    }
                }
            }
        }
        
        return buildPaginatedResponse(allItems, page, size, sortDir);
    }

    /**
     * Récupère les factures et dépenses pour une caisse spécifique
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getFacturesEtDepensesByCaisse(
            Long caisseId,
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            String typeFilter,
            HttpServletRequest request) {
        
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        if (!isAdminOrManager) {
            Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse introuvable"));
            
            boolean userOwnsCaisse = caisse.getVendeur() != null && 
                                   caisse.getVendeur().getId().equals(user.getId());
            if (!userOwnsCaisse) {
                throw new RuntimeException("Vous ne pouvez accéder qu'aux données de votre caisse.");
            }
        }
        
        if (dateDebut == null) {
            LocalDate today = LocalDate.now();
            dateDebut = today.withDayOfYear(1).atStartOfDay();
        }
        if (dateFin == null) {
            dateFin = LocalDateTime.now();
        }
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems = new ArrayList<>();
        
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("FACTURE_VENTE")) {
            List<Vente> ventes = venteRepository.findByBoutiqueIdAndDateRange(
                caisseRepository.findById(caisseId).orElseThrow(() -> new RuntimeException("Caisse introuvable")).getBoutique().getId(), 
                dateDebut, dateFin);
            
            for (Vente vente : ventes) {
                if (vente.getCaisse() == null || !vente.getCaisse().getId().equals(caisseId) ||
                    !vente.getBoutique().getEntreprise().getId().equals(entrepriseId)) {
                    continue;
                }
                
                FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                item.setId(vente.getId());
                item.setType("FACTURE_VENTE");
                item.setDescription("Vente - " + (vente.getClientNom() != null ? vente.getClientNom() : "Client passant"));
                item.setMontant(vente.getMontantTotal());
                item.setDate(vente.getDateVente());
                item.setBoutique(vente.getBoutique().getNomBoutique());
                item.setVendeur(vente.getVendeur().getNomComplet());
                item.setCaisseId(vente.getCaisse().getId());
                item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
                item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
                item.setClientNom(vente.getClientNom());
                item.setClientNumero(vente.getClientNumero());
                item.setRemiseGlobale(vente.getRemiseGlobale());
                
                Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                        ? vente.getBoutique().getEntreprise().getId() : null;
                Optional<FactureVente> factureVente = venteEntrepriseId != null 
                        ? factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                        : Optional.empty();
                if (factureVente.isPresent()) {
                    item.setNumeroFactureVente(factureVente.get().getNumeroFacture());
                }
                
                if (vente.getProduits() != null) {
                    List<FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO> produitsDTO = 
                        vente.getProduits().stream().map(vp -> {
                            FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO produitDTO = 
                                new FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO();
                            produitDTO.setProduitId(vp.getProduit().getId());
                            produitDTO.setNomProduit(vp.getProduit().getNom());
                            if (vp.isEstRemboursee() && vp.getQuantiteRemboursee() != null && vp.getQuantiteRemboursee() > 0) {
                                produitDTO.setQuantite(vp.getQuantiteRemboursee());
                            } else {
                                produitDTO.setQuantite(vp.getQuantite());
                            }
                            produitDTO.setPrixUnitaire(vp.getPrixUnitaire());
                            produitDTO.setMontantLigne(vp.getMontantLigne());
                            produitDTO.setRemise(vp.getRemise());
                            return produitDTO;
                        }).collect(Collectors.toList());
                    item.setProduits(produitsDTO);
                }
                
                allItems.add(item);
            }
        }
        
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("DEPENSE")) {
            List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.DEPENSE);
            
            for (MouvementCaisse mouvement : mouvements) {
                if (mouvement.getDateMouvement().isAfter(dateDebut) && 
                    mouvement.getDateMouvement().isBefore(dateFin)) {
                    
                    FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
                    item.setId(mouvement.getId());
                    item.setType("DEPENSE");
                    item.setDescription(mouvement.getDescription());
                    item.setMontant(mouvement.getMontant());
                    item.setDate(mouvement.getDateMouvement());
                    item.setBoutique(mouvement.getCaisse().getBoutique().getNomBoutique());
                    item.setVendeur(mouvement.getCaisse().getVendeur().getNomComplet());
                    item.setCaisseId(mouvement.getCaisse().getId());
                    item.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
                    item.setStatut("EFFECTUE");
                    item.setMotifDepense(mouvement.getDescription());
                    item.setTypeMouvement(mouvement.getTypeMouvement().name());
                    
                    allItems.add(item);
                }
            }
        }
        
        return buildPaginatedResponse(allItems, page, size, sortDir);
    }

    /**
     * Méthode utilitaire pour construire la réponse paginée
     */
    private FactureDepensePaginatedDTO buildPaginatedResponse(
            List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems,
            int page, 
            int size, 
            String sortDir) {
        
        allItems.sort((item1, item2) -> {
            if (sortDir.equalsIgnoreCase("desc")) {
                return item2.getDate().compareTo(item1.getDate());
            } else {
                return item1.getDate().compareTo(item2.getDate());
            }
        });
        
        double totalFacturesVente = allItems.stream()
            .filter(item -> "FACTURE_VENTE".equals(item.getType()))
            .mapToDouble(item -> item.getMontant() != null ? item.getMontant() : 0.0)
            .sum();
        
        double totalDepenses = allItems.stream()
            .filter(item -> "DEPENSE".equals(item.getType()) || "MOUVEMENT_CAISSE".equals(item.getType()))
            .mapToDouble(item -> item.getMontant() != null ? item.getMontant() : 0.0)
            .sum();
        
        int nombreFacturesVente = (int) allItems.stream()
            .filter(item -> "FACTURE_VENTE".equals(item.getType()))
            .count();
        
        int nombreDepenses = (int) allItems.stream()
            .filter(item -> "DEPENSE".equals(item.getType()) || "MOUVEMENT_CAISSE".equals(item.getType()))
            .count();
        
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> pagedItems = 
            startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
        
        FactureDepensePaginatedDTO response = new FactureDepensePaginatedDTO();
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1);
        response.setHasNext(page < totalPages - 1);
        response.setHasPrevious(page > 0);
        response.setTotalFacturesVente(totalFacturesVente);
        response.setTotalDepenses(totalDepenses);
        response.setSoldeNet(totalFacturesVente - totalDepenses);
        response.setNombreFacturesVente(nombreFacturesVente);
        response.setNombreDepenses(nombreDepenses);
        response.setItems(pagedItems);
        
        return response;
    }

    /**
     * Récupère toutes les factures de vente d'une entreprise
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getAllFacturesVente(
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            HttpServletRequest request) {
        
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.COMPTABILITE) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        if (dateDebut == null) {
            LocalDate today = LocalDate.now();
            dateDebut = today.withDayOfYear(1).atStartOfDay();
        }
        if (dateFin == null) {
            dateFin = LocalDateTime.now();
        }
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems = new ArrayList<>();
        
        List<Vente> ventes;
        
        if (!isAdminOrManager) {
            ventes = venteRepository.findByVendeur_IdAndDateVenteBetween(user.getId(), dateDebut, dateFin);
        } else {
            ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, dateDebut, dateFin);
        }
        
        for (Vente vente : ventes) {
            FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
            item.setId(vente.getId());
            item.setType("FACTURE_VENTE");
            item.setDescription("Vente - " + (vente.getClientNom() != null ? vente.getClientNom() : "Client passant"));
            item.setMontant(vente.getMontantTotal());
            item.setDate(vente.getDateVente());
            item.setCaisseId(vente.getCaisse().getId());
            item.setBoutique(vente.getBoutique().getNomBoutique());
            item.setVendeur(vente.getVendeur().getNomComplet());
            item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
            item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
            item.setClientNom(vente.getClientNom());
            item.setClientNumero(vente.getClientNumero());
            item.setRemiseGlobale(vente.getRemiseGlobale());
            
            Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                    ? vente.getBoutique().getEntreprise().getId() : null;
            Optional<FactureVente> factureVente = venteEntrepriseId != null 
                    ? factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                    : Optional.empty();
            if (factureVente.isPresent()) {
                item.setNumeroFactureVente(factureVente.get().getNumeroFacture());
            }
            
            // Récupérer les produits de la vente
            if (vente.getProduits() != null) {
                List<FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO> produitsDTO = 
                    vente.getProduits().stream().map(vp -> {
                        FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO produitDTO = 
                            new FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO();
                        produitDTO.setProduitId(vp.getProduit().getId());
                        produitDTO.setNomProduit(vp.getProduit().getNom());
                        produitDTO.setQuantite(vp.getQuantite());
                        produitDTO.setPrixUnitaire(vp.getPrixUnitaire());
                        produitDTO.setMontantLigne(vp.getMontantLigne());
                        produitDTO.setRemise(vp.getRemise());
                        return produitDTO;
                    }).collect(Collectors.toList());
                item.setProduits(produitsDTO);
            }
            
            allItems.add(item);
        }
        
        return buildPaginatedResponseFacturesVente(allItems, page, size, sortDir);
    }

    /**
     * Récupère toutes les factures de vente d'une boutique spécifique
     */
    @Transactional(readOnly = true)
    public FactureDepensePaginatedDTO getFacturesVenteByBoutique(
            Long boutiqueId,
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            HttpServletRequest request) {
        
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        if (!isAdminOrManager) {
            boolean userBelongsToBoutique = user.getUserBoutiques().stream()
                .anyMatch(ub -> ub.getBoutique().getId().equals(boutiqueId));
            if (!userBelongsToBoutique) {
                throw new RuntimeException("Vous ne pouvez accéder qu'aux données de votre boutique.");
            }
        }
        
        if (dateDebut == null) {
            LocalDate today = LocalDate.now();
            dateDebut = today.withDayOfYear(1).atStartOfDay();
        }
        if (dateFin == null) {
            dateFin = LocalDateTime.now();
        }
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems = new ArrayList<>();
        
        List<Vente> ventes = venteRepository.findByBoutiqueIdAndDateRange(boutiqueId, dateDebut, dateFin);
        
        for (Vente vente : ventes) {
            if (!vente.getBoutique().getEntreprise().getId().equals(entrepriseId)) {
                continue;
            }
            
            FactureDepensePaginatedDTO.FactureDepenseItemDTO item = new FactureDepensePaginatedDTO.FactureDepenseItemDTO();
            item.setId(vente.getId());
            item.setType("FACTURE_VENTE");
            item.setDescription("Vente - " + (vente.getClientNom() != null ? vente.getClientNom() : "Client passant"));
            item.setMontant(vente.getMontantTotal());
            item.setDate(vente.getDateVente());
            item.setCaisseId(vente.getCaisse().getId());
            item.setBoutique(vente.getBoutique().getNomBoutique());
            item.setVendeur(vente.getVendeur().getNomComplet());
            item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
            item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
            item.setClientNom(vente.getClientNom());
            item.setClientNumero(vente.getClientNumero());
            item.setRemiseGlobale(vente.getRemiseGlobale());
            
            Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                    ? vente.getBoutique().getEntreprise().getId() : null;
            Optional<FactureVente> factureVente = venteEntrepriseId != null 
                    ? factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                    : Optional.empty();
            if (factureVente.isPresent()) {
                item.setNumeroFactureVente(factureVente.get().getNumeroFacture());
            }
            
            if (vente.getProduits() != null) {
                List<FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO> produitsDTO = 
                    vente.getProduits().stream().map(vp -> {
                        FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO produitDTO = 
                            new FactureDepensePaginatedDTO.FactureDepenseItemDTO.VenteProduitDTO();
                        produitDTO.setProduitId(vp.getProduit().getId());
                        produitDTO.setNomProduit(vp.getProduit().getNom());
                        produitDTO.setQuantite(vp.getQuantite());
                        produitDTO.setPrixUnitaire(vp.getPrixUnitaire());
                        produitDTO.setMontantLigne(vp.getMontantLigne());
                        produitDTO.setRemise(vp.getRemise());
                        return produitDTO;
                    }).collect(Collectors.toList());
                item.setProduits(produitsDTO);
            }
            
            allItems.add(item);
        }
        
        return buildPaginatedResponseFacturesVente(allItems, page, size, sortDir);
    }

    /**
     * Méthode utilitaire pour construire la réponse paginée pour les factures de vente uniquement
     */
    private FactureDepensePaginatedDTO buildPaginatedResponseFacturesVente(
            List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> allItems,
            int page, 
            int size, 
            String sortDir) {
        
        allItems.sort((item1, item2) -> {
            if (sortDir.equalsIgnoreCase("desc")) {
                return item2.getDate().compareTo(item1.getDate());
            } else {
                return item1.getDate().compareTo(item2.getDate());
            }
        });
        
        double totalFacturesVente = allItems.stream()
            .mapToDouble(item -> item.getMontant() != null ? item.getMontant() : 0.0)
            .sum();
        
        double totalDepenses = 0.0;
        int nombreFacturesVente = allItems.size();
        int nombreDepenses = 0; 
        
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> pagedItems = 
            startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
        
        FactureDepensePaginatedDTO response = new FactureDepensePaginatedDTO();
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1);
        response.setHasNext(page < totalPages - 1);
        response.setHasPrevious(page > 0);
        response.setTotalFacturesVente(totalFacturesVente);
        response.setTotalDepenses(totalDepenses);
        response.setSoldeNet(totalFacturesVente - totalDepenses);
        response.setNombreFacturesVente(nombreFacturesVente);
        response.setNombreDepenses(nombreDepenses);
        response.setItems(pagedItems);
        
        return response;
    }
}
