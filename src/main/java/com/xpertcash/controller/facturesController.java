package com.xpertcash.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> getAllFactures() {
        List<Facture> factures = factureRepository.findAll();

        if (factures.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Aucune facture disponible.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<FactureDTO> factureDTOS = factures.stream()
            .map(FactureDTO::new)
            .collect(Collectors.toList());

        return ResponseEntity.ok(factureDTOS);
    }



}
