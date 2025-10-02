package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiviteHebdoDTO {
    private List<String> dates;  // Format "DD/MM"
    private List<Double> ventes;
    private List<Double> facturesReelles;
    private List<Double> facturesProforma;
}

