package com.xpertcash.DTOs;

import java.util.Map;

import lombok.Data;

@Data
public class RetirerStockRequest {
    private Map<Long, Integer> produitsQuantites;
    private String description;
}