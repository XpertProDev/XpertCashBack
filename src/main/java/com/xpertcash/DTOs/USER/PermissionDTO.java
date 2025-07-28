package com.xpertcash.DTOs.USER;

import lombok.Data;

@Data
public class PermissionDTO {
    private Long id;
    private String type;

    // Constructeur
    public PermissionDTO(Long id, String type) {
        this.id = id;
        this.type = type;
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

