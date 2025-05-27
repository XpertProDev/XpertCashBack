package com.xpertcash.controller;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xpertcash.service.FactProHistoriqueService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactProHistoriqueActionController {
    @Autowired
    private FactProHistoriqueService factProHistoriqueService;
  


    @GetMapping("/factpro/{id}/historique")
    public ResponseEntity<?> getHistoriqueFacture(@PathVariable Long id, HttpServletRequest request) {
        try {
            Map<String, Object> historique = factProHistoriqueService.getHistoriqueFacture(id, request);
            return ResponseEntity.ok(historique);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

}
