package com.xpertcash.DTOs.USER;

import java.util.List;

import lombok.Data;

@Data
public class RoleDTO {
    private Long id;
    private String name;
    private List<PermissionDTO> permissions;

    // Constructeur
    public RoleDTO(Long id, String name, List<PermissionDTO> permissions) {
        this.id = id;
        this.name = name;
        this.permissions = permissions;
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }
}

