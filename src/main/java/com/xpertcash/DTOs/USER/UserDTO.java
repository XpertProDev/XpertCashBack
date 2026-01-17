package com.xpertcash.DTOs.USER;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class UserDTO {
    private Long id;
    private String uuid;
    private String personalCode;
    private String nomComplet;
    private String email;
    private String phone;
    private String pays;
    private String photo;
    private String createdAt;
    private String activationCode;
    private Boolean activatedLien;
    private Boolean enabledLien;
    private String lastActivity;
    private Boolean locked;
    private RoleDTO role;
    private List<UserBoutiqueDTO> userBoutiques;

    // Constructeurs, getters, setters...

    public UserDTO(Long id, String uuid, String personalCode, String nomComplet, String email, String phone, 
                   String pays, String photo, String createdAt, String activationCode, 
                   Boolean activatedLien, Boolean enabledLien, String lastActivity, 
                   Boolean locked, RoleDTO role, List<UserBoutiqueDTO> userBoutiques) {
        this.id = id;
        this.uuid = uuid;
        this.personalCode = personalCode;
        this.nomComplet = nomComplet;
        this.email = email;
        this.phone = phone;
        this.pays = pays;
        this.photo = photo;
        this.createdAt = createdAt;
        this.activationCode = activationCode;
        this.activatedLien = activatedLien;
        this.enabledLien = enabledLien;
        this.lastActivity = lastActivity;
        this.locked = locked;
        this.role = role;
        this.userBoutiques = userBoutiques;
    }

}
