package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Transfert;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.TransfertRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class BoutiqueService {

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TransfertRepository transfertRepository;
    
    @Autowired
    private ProduitRepository produitRepository;


    // Ajouter une nouvelle boutique pour l'admin
    @Transactional
    public Boutique ajouterBoutique(HttpServletRequest request, String nomBoutique, String adresse, String Telephone, String email) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'admin depuis le token
        Long adminId = null;
        try {
            adminId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
        }
           // Récupérer l'admin par son ID
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'admin est bien un Admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut ajouter une boutique !");
        }

        // Vérifier que l'admin possède une entreprise
        if (admin.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        // Créer une nouvelle boutique pour l'entreprise de l'admin
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setAdresse(adresse);
        boutique.setTelephone(Telephone);
        boutique.setEmail(email);
        boutique.setEntreprise(admin.getEntreprise());
        boutique.setCreatedAt(LocalDateTime.now());

        // Sauvegarder la boutique en base de données
        return boutiqueRepository.save(boutique);
    }


    // Récupérer toutes les boutiques d'une entreprise
    public List<Boutique> getBoutiquesByEntreprise(HttpServletRequest request) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'utilisateur depuis le token
        Long userId = null;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
        }

        // Récupérer l'utilisateur par son ID
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que l'utilisateur est bien un admin et qu'il a une entreprise associée
        if (user.getRole() == null || !user.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut récupérer les boutiques d'une entreprise !");
        }

        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        // Récupérer et retourner toutes les boutiques de l'entreprise
        return boutiqueRepository.findByEntrepriseId(user.getEntreprise().getId());
    }


    //Methode update de Boutique
    public Boutique updateBoutique(Long boutiqueId, String newNomBoutique, String newAdresse,String newTelephone, String newEmail, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        Long adminId = jwtUtil.extractUserId(token.substring(7));
        System.out.println("ID ADMIN EXTRAIT : " + adminId);
    
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un ADMIN peut modifier une boutique !");
        }
    
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
    
        // Modifier les informations de la boutique
        if (newNomBoutique != null) boutique.setNomBoutique(newNomBoutique);
        if (newAdresse != null) boutique.setAdresse(newAdresse);
        if (newTelephone != null) boutique.setTelephone(newTelephone);
        if (newEmail != null) boutique.setEmail(newEmail);
        boutique.setLastUpdated(LocalDateTime.now());
    
        return boutiqueRepository.save(boutique);
    }
    
    @Transactional
    public void transfererProduits(HttpServletRequest request, Long boutiqueSourceId, Long boutiqueDestinationId, Long produitId, int quantite) {
        // Vérifier la présence du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        // Extraire l'ID de l'admin
        Long adminId = jwtUtil.extractUserId(token.substring(7));
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut transférer des produits !");
        }
    
        // Récupérer les boutiques source et destination
        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));
    
        // Vérifier l'appartenance des boutiques à l'entreprise de l'admin
        if (!boutiqueSource.getEntreprise().equals(admin.getEntreprise()) || 
            !boutiqueDestination.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Les boutiques doivent appartenir à l'entreprise de l'admin !");
        }
    
        // Vérifier que le produit existe et que la quantité est suffisante
        Produit produit = produitRepository.findByBoutiqueAndId(boutiqueSourceId, produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé dans la boutique source"));
    
        if (produit.getQuantite() < quantite) {
            throw new RuntimeException("Quantité insuffisante dans la boutique source !");
        }
    
        // Réduire la quantité dans la boutique source
        produit.setQuantite(produit.getQuantite() - quantite);
    
        // Vérifier si le produit existe dans la boutique destination
        Optional<Produit> produitDestinationOpt = produitRepository.findByBoutiqueAndCodeGenerique(boutiqueDestination.getId(), produit.getCodeGenerique());
    
        if (produitDestinationOpt.isPresent()) {
            Produit produitDestination = produitDestinationOpt.get();
            produitDestination.setQuantite(produitDestination.getQuantite() + quantite);
        } else {
            Produit nouveauProduit = new Produit();
            nouveauProduit.setNom(produit.getNom());
            nouveauProduit.setPrixVente(produit.getPrixVente());
            nouveauProduit.setPrixAchat(produit.getPrixAchat());
            nouveauProduit.setQuantite(quantite);
            nouveauProduit.setCodeGenerique(produit.getCodeGenerique());
            nouveauProduit.setCodeBare(produit.getCodeBare());
            nouveauProduit.setPhoto(produit.getPhoto());
            nouveauProduit.setCategorie(produit.getCategorie());
            nouveauProduit.setUniteDeMesure(produit.getUniteDeMesure());
            nouveauProduit.setCreatedAt(produit.getCreatedAt());
            nouveauProduit.setLastUpdated(produit.getLastUpdated());
            nouveauProduit.setBoutique(boutiqueDestination);
            produitRepository.save(nouveauProduit);
        }
    
        // Enregistrer le transfert
        Transfert transfert = new Transfert();
        transfert.setProduit(produit);
        transfert.setBoutiqueSource(boutiqueSource);
        transfert.setBoutiqueDestination(boutiqueDestination);
        transfert.setQuantite(quantite);
        transfertRepository.save(transfert);
    }

    
    public List<Produit> getProduitsParBoutique(HttpServletRequest request, Long boutiqueId) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'utilisateur depuis le token
        Long userId = jwtUtil.extractUserId(token.substring(7));

        // Récupérer l'utilisateur (admin)
        User admin = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'utilisateur est bien un admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut accéder aux produits !");
        }

        // Récupérer la boutique par son ID
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        // Vérifier que la boutique appartient à l'entreprise de l'admin
        if (!boutique.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Vous n'avez pas accès à cette boutique !");
        }

        // Retourner la liste des produits de la boutique
        return boutique.getProduits();
    }

}
