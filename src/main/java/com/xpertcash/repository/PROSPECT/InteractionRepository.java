package com.xpertcash.repository.PROSPECT;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.PROSPECT.Interaction;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    List<Interaction> findByProspectIdOrderByOccurredAtDesc(Long prospectId);
    Optional<Interaction> findByIdAndProspectEntrepriseId(Long id, Long entrepriseId);
    
    // Récupérer les interactions d'un client converti (avec vérification du type)
    List<Interaction> findByProspectClientIdAndProspectClientTypeOrderByOccurredAtDesc(Long clientId, String clientType);

}
