package com.xpertcash.DTOs.USER;

import lombok.Data;

@Data
public class PermissionDTO {
    private Long id;
    private String type;

    public PermissionDTO(Long id, String type) {
        this.id = id;
        this.type = type;
    }

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

