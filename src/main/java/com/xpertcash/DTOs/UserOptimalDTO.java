package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOptimalDTO {
    private UserInfoDTO userInfo;
    private List<RoleDTO> roles;
    private List<BoutiqueDTO> boutiques;
    private List<UserDTO> users;
    private String currentUserRole;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfoDTO {
        private Long id;
        private String nomComplet;
        private String email;
        private String roleType;
        private String uuid;
        private String pays;
        private String phone;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoleDTO {
        private Long id;
        private String name;
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BoutiqueDTO {
        private Long id;
        private String nom;
        private String adresse;
        private String telephone;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDTO {
        private Long id;
        private String nomComplet;
        private String email;
        private String roleType;
        private String uuid;
        private String pays;
        private String phone;
        private RoleDTO role;
        private List<PermissionDTO> permissions;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PermissionDTO {
        private Long id;
        private String type;
        private String description;
    }
}
