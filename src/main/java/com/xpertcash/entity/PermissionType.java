package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PermissionType {
    GERER_PRODUITS,
    VENDRE_PRODUITS,
    APPROVISIONNER_STOCK,
    GESTION_FACTURATION,
    GERER_CLIENTS,
    GERER_FOURNISSEURS,
    GERER_UTILISATEURS,
    GERER_BOUTIQUE,
    ACTIVER_BOUTIQUE,
    DESACTIVER_BOUTIQUE,
    COMPTABILITE,
    VOIR_FLUX_COMPTABLE,
    GERER_MARKETING;


    @JsonCreator
    public static PermissionType fromString(String value) {
        try {
            return PermissionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("PermissionType invalide : " + value);
        }
    }

}
