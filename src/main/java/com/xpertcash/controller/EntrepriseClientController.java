package com.xpertcash.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.service.EntrepriseClientService;

@RestController
@RequestMapping("/api/auth")
public class EntrepriseClientController {

    @Autowired
    private EntrepriseClientService entrepriseClientService;

    @PostMapping("/entreprises")
    public EntrepriseClient createEntreprise(@RequestBody EntrepriseClient entrepriseClient) {
        return entrepriseClientService.saveEntreprise(entrepriseClient);
    }

    @GetMapping("/entreprises/{id}")
    public Optional<EntrepriseClient> getEntrepriseById(@PathVariable Long id) {
        return entrepriseClientService.getEntrepriseById(id);
    }

    @GetMapping("/entreprises")
    public List<EntrepriseClient> getAllEntreprises() {
        return entrepriseClientService.getAllEntreprises();
    }

    @DeleteMapping("/entreprises/{id}")
    public void deleteEntreprise(@PathVariable Long id) {
        entrepriseClientService.deleteEntreprise(id);
    }
}
