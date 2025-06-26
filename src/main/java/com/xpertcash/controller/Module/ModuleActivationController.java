package com.xpertcash.controller.Module;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.Module.ModuleDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.AppModule;
import com.xpertcash.service.Module.ModuleActivationService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class ModuleActivationController {
    @Autowired
    private ModuleActivationService moduleActivationService;
    
    @Autowired
    private JwtUtil jwtUtil; 
    

        /**
     * Endpoint pour activer un module
     */
    @PostMapping("/modules/activer")
    public ResponseEntity<?> activerModule(@RequestParam String nomModule, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

        moduleActivationService.activerModule(userId, nomModule);

        return ResponseEntity.ok("Le module '" + nomModule + "' a été activé avec succès.");
    }


    //Endpoint pour lister tout les  modules actifs ou non actifs
    @GetMapping("/entreprise/modules")
    public ResponseEntity<List<ModuleDTO>> getModulesEntreprise(HttpServletRequest request) {
        List<ModuleDTO> modules = moduleActivationService.listerModulesEntreprise(request);
        return ResponseEntity.ok(modules);
    }

 

}
