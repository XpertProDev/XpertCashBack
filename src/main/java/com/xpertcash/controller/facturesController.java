package com.xpertcash.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.entity.Facture;
import com.xpertcash.repository.FactureRepository;

@RestController
@RequestMapping("/api/auth")
public class facturesController {

    @Autowired
    private FactureRepository factureRepository;

    @GetMapping("/factures")
    public ResponseEntity<List<FactureDTO>> getAllFactures() {
        List<Facture> factures = factureRepository.findAll();
        
        List<FactureDTO> factureDTOS = factures.stream()
            .map(FactureDTO::new)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(factureDTOS);
    }


}
