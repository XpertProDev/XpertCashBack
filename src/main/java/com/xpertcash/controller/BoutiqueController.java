package com.xpertcash.controller;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.BoutiqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boutiques")
public class BoutiqueController {

    @Autowired
    private BoutiqueService boutiqueService;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/create")
    public ResponseEntity<Boutique> createBoutique(@RequestBody Boutique boutique, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));  // Extraction de l'ID utilisateur à partir du token

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Boutique createdBoutique = boutiqueService.createBoutiqueForUser(user, boutique.getNomBoutique());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBoutique);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Boutique> updateBoutique(@PathVariable Long id, @RequestBody Boutique boutique, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));  // Extraction de l'ID utilisateur à partir du token

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Boutique updatedBoutique = boutiqueService.updateBoutiqueName(id, boutique.getNomBoutique());
        return ResponseEntity.status(HttpStatus.OK).body(updatedBoutique);
    }
}
