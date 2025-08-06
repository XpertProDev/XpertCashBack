package com.xpertcash.service.VENTE;

import com.xpertcash.entity.*;
import com.xpertcash.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import com.xpertcash.configuration.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.xpertcash.entity.Enum.RoleType;

@Service
public class CaisseService {
    @Autowired
    private CaisseRepository caisseRepository;
    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private JwtUtil jwtUtil;

        private User getUserFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String jwtToken = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwtToken);
        return usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }


   @Transactional
    public Caisse ouvrirCaisse(Long boutiqueId, Double montantInitial, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        // Sécurité : rôle ou permission
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour ouvrir une caisse !");
        }

        // Vérification d'appartenance à l'entreprise
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Vérifier qu'il n'y a pas déjà une caisse ouverte pour ce vendeur/boutique
        if (caisseRepository.existsByBoutiqueIdAndVendeurIdAndStatut(boutiqueId, user.getId(), StatutCaisse.OUVERTE)) {
            throw new RuntimeException("Une caisse est déjà ouverte pour ce vendeur dans cette boutique.");
        }

        // Création d'une nouvelle caisse (historique)
        Caisse caisse = new Caisse();
        caisse.setBoutique(boutique);
        caisse.setVendeur(user);
        caisse.setMontantInitial(montantInitial != null ? montantInitial : 0.0);
        caisse.setMontantCourant(montantInitial != null ? montantInitial : 0.0);
        caisse.setStatut(StatutCaisse.OUVERTE);
        caisse.setDateOuverture(LocalDateTime.now());

        return caisseRepository.save(caisse);
    }

    @Transactional
    public Caisse fermerCaisse(Long caisseId, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        // On charge la caisse seulement si elle est OUVERTE
        Caisse caisse = caisseRepository.findByIdAndStatut(caisseId, StatutCaisse.OUVERTE)
                .orElseThrow(() -> new RuntimeException("Caisse introuvable ou déjà fermée."));

        // Sécurité : rôle ou permission
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour fermer une caisse !");
        }

        // Vérification d'appartenance à l'entreprise
        if (!caisse.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Autorisation : seul le vendeur ou un admin/manager peut fermer
        if (!isAdminOrManager && !caisse.getVendeur().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à fermer cette caisse.");
        }

        // Mise à jour du statut et date fermeture
        caisse.setStatut(StatutCaisse.FERMEE);
        caisse.setDateFermeture(LocalDateTime.now());
        caisseRepository.save(caisse);

        // Enregistrement d'un mouvement de fermeture
        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setCaisse(caisse);
        mouvement.setTypeMouvement(TypeMouvementCaisse.FERMETURE);
        mouvement.setMontant(caisse.getMontantCourant());
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setDescription("Fermeture de la caisse");
        mouvementCaisseRepository.save(mouvement);

        return caisse;
    }

  
    //Get caisse ouvert
    public Optional<Caisse> getCaisseActive(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    // Sécurité : rôle ou permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter la caisse !");
    }

    // Vérification d'appartenance à l'entreprise
    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    // Requête optimisée
    return caisseRepository.findFirstByBoutiqueIdAndVendeurIdAndStatut(
            boutiqueId,
            user.getId(),
            StatutCaisse.OUVERTE
    );
}


    //Get tout les caisse ouvert
    public List<Caisse> getCaissesActivesBoutique(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    // Sécurité : rôle ou permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses !");
    }

    // Vérification d'appartenance à l'entreprise
    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    // Récupération optimisée depuis la base
    return caisseRepository.findByBoutiqueIdAndStatut(boutiqueId, StatutCaisse.OUVERTE);
}


    //Get tout les caisse
    public List<Caisse> getToutesLesCaisses(Long boutiqueId, HttpServletRequest request) {
        User user = getUserFromRequest(request);
        
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        // Vérification d'appartenance à l'entreprise
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Sécurité : rôle ou permission
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses !");
        }

        return caisseRepository.findByBoutiqueId(boutiqueId);
    }


    @Transactional
    public void ajouterMouvement(Caisse caisse, TypeMouvementCaisse type, Double montant, String description, Vente vente, ModePaiement modePaiement, Double montantPaye) {
        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setCaisse(caisse);
        mouvement.setTypeMouvement(type);
        mouvement.setMontant(montant);
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setDescription(description);
        mouvement.setVente(vente);
        mouvement.setModePaiement(modePaiement);
        mouvement.setMontantPaye(montantPaye);
        mouvementCaisseRepository.save(mouvement);
        // Mettre à jour le montant courant de la caisse
        if (type == TypeMouvementCaisse.VENTE || type == TypeMouvementCaisse.AJOUT) {
            caisse.setMontantCourant(caisse.getMontantCourant() + montant);
        } else if (type == TypeMouvementCaisse.RETRAIT) {
            caisse.setMontantCourant(caisse.getMontantCourant() - montant);
        }
        caisseRepository.save(caisse);
    }




}