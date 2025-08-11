package com.xpertcash.DTOs.VENTE;

import java.util.Map;

import lombok.Data;

@Data
public class RemboursementRequest {
    private Long venteId;
    private Map<Long, Integer> produitsQuantites;
    private String motif;
    private String rescodePin;

}
