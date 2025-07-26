package com.xpertcash.DTOs.Boutique;

import java.util.List;

import lombok.Data;

@Data
public class AssignerVendeurRequest {
    private Long userId;
    private List<Long> boutiqueIds;
}
