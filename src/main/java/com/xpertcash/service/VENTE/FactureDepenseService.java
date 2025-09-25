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
            String typeFilter, // "FACTURE_VENTE", "DEPENSE", ou "ALL"
            HttpServletRequest request) {
        
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Vérification des droits
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
        
        // 1. RÉCUPÉRER LES FACTURES DE VENTE
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("FACTURE_VENTE")) {
            List<Vente> ventes;
            
            // Si c'est un vendeur, ne récupérer que ses ventes
            if (!isAdminOrManager) {
                ventes = venteRepository.findByVendeur_IdAndDateVenteBetween(user.getId(), dateDebut, dateFin);
            } else {
                // Admin/Manager : toutes les ventes de l'entreprise
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
                item.setUtilisateur(vente.getVendeur().getNomComplet());
                item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
                item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
                item.setClientNom(vente.getClientNom());
                item.setClientNumero(vente.getClientNumero());
                item.setRemiseGlobale(vente.getRemiseGlobale());
                
                // Récupérer le numéro de facture de vente
                Optional<FactureVente> factureVente = factureVenteRepository.findByVente(vente);
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
        }
        
        // 2. RÉCUPÉRER LES DÉPENSES (MOUVEMENTS DE CAISSE)
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("DEPENSE")) {
            List<MouvementCaisse> depenses;
            
            // Si c'est un vendeur, ne récupérer que ses dépenses
            if (!isAdminOrManager) {
                // Récupérer les caisses de l'utilisateur
                List<Caisse> userCaisses = caisseRepository.findByVendeur_Id(user.getId());
                List<Long> caisseIds = userCaisses.stream().map(Caisse::getId).collect(Collectors.toList());
                
                if (!caisseIds.isEmpty()) {
                    depenses = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvementAndDateMouvementBetween(
                        caisseIds, TypeMouvementCaisse.DEPENSE, dateDebut, dateFin);
                } else {
                    depenses = new ArrayList<>();
                }
            } else {
                // Admin/Manager : toutes les dépenses de l'entreprise
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
                item.setUtilisateur(depense.getCaisse().getVendeur().getNomComplet());
                item.setModePaiement(depense.getModePaiement() != null ? depense.getModePaiement().name() : null);
                item.setStatut("EFFECTUE");
                item.setMotifDepense(depense.getDescription());
                item.setTypeMouvement(depense.getTypeMouvement().name());
                
                allItems.add(item);
            }
        }
        
        // 3. RÉCUPÉRER LES AUTRES MOUVEMENTS DE CAISSE (AJOUTS, RETRAITS, REMBOURSEMENTS)
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
                    item.setUtilisateur(mouvement.getCaisse().getVendeur().getNomComplet());
                    item.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
                    item.setStatut("EFFECTUE");
                    item.setTypeMouvement(mouvement.getTypeMouvement().name());
                    
                    allItems.add(item);
                }
            }
        }
        
        // 4. TRIER TOUS LES ÉLÉMENTS PAR DATE
        allItems.sort((item1, item2) -> {
            if (sortDir.equalsIgnoreCase("desc")) {
                return item2.getDate().compareTo(item1.getDate());
            } else {
                return item1.getDate().compareTo(item2.getDate());
            }
        });
        
        // 5. CALCULER LES TOTAUX
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
        
        // 6. APPLIQUER LA PAGINATION MANUELLE
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> pagedItems = 
            startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
        
        // 7. CONSTRUIRE LA RÉPONSE
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
        
        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        // Vérification spécifique pour les vendeurs : ils ne peuvent voir que leur boutique
        if (!isAdminOrManager) {
            // Vérifier que l'utilisateur appartient à cette boutique via UserBoutique
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
        
        // 1. RÉCUPÉRER LES FACTURES DE VENTE POUR CETTE BOUTIQUE
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("FACTURE_VENTE")) {
            List<Vente> ventes = venteRepository.findByBoutiqueIdAndDateRange(boutiqueId, dateDebut, dateFin);
            
            for (Vente vente : ventes) {
                // Vérifier que la vente appartient à l'entreprise de l'utilisateur
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
                item.setUtilisateur(vente.getVendeur().getNomComplet());
                item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
                item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
                item.setClientNom(vente.getClientNom());
                item.setClientNumero(vente.getClientNumero());
                item.setRemiseGlobale(vente.getRemiseGlobale());
                
                // Récupérer le numéro de facture de vente
                Optional<FactureVente> factureVente = factureVenteRepository.findByVente(vente);
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
        }
        
        // 2. RÉCUPÉRER LES MOUVEMENTS DE CAISSE POUR CETTE BOUTIQUE
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("DEPENSE")) {
            List<Caisse> caisses = caisseRepository.findByBoutiqueId(boutiqueId);
            
            for (Caisse caisse : caisses) {
                // Vérifier que la caisse appartient à l'entreprise de l'utilisateur
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
                        item.setUtilisateur(mouvement.getCaisse().getVendeur().getNomComplet());
                        item.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
                        item.setStatut("EFFECTUE");
                        item.setMotifDepense(mouvement.getDescription());
                        item.setTypeMouvement(mouvement.getTypeMouvement().name());
                        
                        allItems.add(item);
                    }
                }
            }
        }
        
        // Appliquer la pagination et retourner le résultat
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
        
        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION) || 
                               user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces informations.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        // Vérification spécifique pour les vendeurs : ils ne peuvent voir que leur caisse
        if (!isAdminOrManager) {
            // Récupérer la caisse pour vérifier l'appartenance
            Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse introuvable"));
            
            // Vérifier que l'utilisateur est le vendeur de cette caisse
            boolean userOwnsCaisse = caisse.getVendeur() != null && 
                                   caisse.getVendeur().getId().equals(user.getId());
            if (!userOwnsCaisse) {
                throw new RuntimeException("Vous ne pouvez accéder qu'aux données de votre caisse.");
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
        
        // 1. RÉCUPÉRER LES VENTES POUR CETTE CAISSE
        if (typeFilter == null || typeFilter.equals("ALL") || typeFilter.equals("FACTURE_VENTE")) {
            List<Vente> ventes = venteRepository.findByBoutiqueIdAndDateRange(
                caisseRepository.findById(caisseId).orElseThrow(() -> new RuntimeException("Caisse introuvable")).getBoutique().getId(), 
                dateDebut, dateFin);
            
            for (Vente vente : ventes) {
                // Vérifier que la vente appartient à cette caisse et à l'entreprise
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
                item.setUtilisateur(vente.getVendeur().getNomComplet());
                item.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
                item.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
                item.setClientNom(vente.getClientNom());
                item.setClientNumero(vente.getClientNumero());
                item.setRemiseGlobale(vente.getRemiseGlobale());
                
                // Récupérer le numéro de facture de vente
                Optional<FactureVente> factureVente = factureVenteRepository.findByVente(vente);
                if (factureVente.isPresent()) {
                    item.setNumeroFactureVente(factureVente.get().getNumeroFacture());
                }
                
                allItems.add(item);
            }
        }
        
        // 2. RÉCUPÉRER LES MOUVEMENTS DE CETTE CAISSE
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
                    item.setUtilisateur(mouvement.getCaisse().getVendeur().getNomComplet());
                    item.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
                    item.setStatut("EFFECTUE");
                    item.setMotifDepense(mouvement.getDescription());
                    item.setTypeMouvement(mouvement.getTypeMouvement().name());
                    
                    allItems.add(item);
                }
            }
        }
        
        // Appliquer la pagination et retourner le résultat
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
        
        // Trier tous les éléments par date
        allItems.sort((item1, item2) -> {
            if (sortDir.equalsIgnoreCase("desc")) {
                return item2.getDate().compareTo(item1.getDate());
            } else {
                return item1.getDate().compareTo(item2.getDate());
            }
        });
        
        // Calculer les totaux
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
        
        // Appliquer la pagination manuelle
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<FactureDepensePaginatedDTO.FactureDepenseItemDTO> pagedItems = 
            startIndex < totalElements ? allItems.subList(startIndex, endIndex) : new ArrayList<>();
        
        // Construire la réponse
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
