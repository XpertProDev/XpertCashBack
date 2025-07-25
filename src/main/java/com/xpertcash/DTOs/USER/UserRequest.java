package com.xpertcash.DTOs.USER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.DTOs.BoutiqueResponse;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class UserRequest {

    private long id;
    private String nomComplet;
    private String nomEntreprise;
    private String siege;
    private String email;
    private RoleType roleType;
    private LocalDateTime createdAt;
    private List<String> permissions; 
    private Long boutiqueId;


    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Le téléphone doit être au format valide")
    private String phone;

    private String pays;
    private String adresseEntreprise;
    private String logoEntreprise;
    private Long entrepriseId;
    private List<BoutiqueResponse> boutiques;
    private String personalCode;
    private String photo;
    private boolean userActivated;
    private Boolean adminActivated;
    private LocalDateTime adminCreatedAt;


    // Constructeur principal
   public UserRequest(User user, Entreprise entreprise, List<BoutiqueResponse> boutiques, List<String> permissions) {
    this.id = user.getId();
    this.nomComplet = user.getNomComplet();
    this.nomEntreprise = entreprise.getNomEntreprise();
    this.siege = entreprise.getSiege();
    this.email = user.getEmail();
    this.roleType = user.getRole().getName();
    this.createdAt = user.getCreatedAt();
    this.phone = user.getPhone();
    this.pays = user.getPays();
    this.adresseEntreprise = entreprise.getAdresse();
    this.logoEntreprise = entreprise.getLogo();
    this.entrepriseId = entreprise.getId();
    this.boutiques = boutiques;
    this.permissions = permissions;
    this.personalCode = user.getPersonalCode();
    this.photo = user.getPhoto();
    this.userActivated = user.isEnabledLien();


    // Admin data directement récupérée
    if (user.getEntreprise() != null && user.getEntreprise().getAdmin() != null) {
        this.adminActivated = user.getEntreprise().getAdmin().isActivatedLien();
        this.adminCreatedAt = user.getEntreprise().getAdmin().getCreatedAt();
    }
}

    public UserRequest(User user) {
    this.id = user.getId();
    this.nomComplet = user.getNomComplet();
    this.email = user.getEmail();
    this.roleType = user.getRole().getName();
    this.createdAt = user.getCreatedAt();
    this.phone = user.getPhone();
    this.photo = user.getPhoto();
    this.userActivated = user.isEnabledLien();

    this.adminActivated = user.getEntreprise().getAdmin().isActivatedLien();
    this.adminCreatedAt = user.getEntreprise().getAdmin().getCreatedAt();
    


}


    // Getters & Setters (génère-les si tu utilises Lombok sinon à la main)
}
