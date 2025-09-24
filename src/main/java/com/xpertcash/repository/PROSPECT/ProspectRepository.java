package com.xpertcash.repository.PROSPECT;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Enum.PROSPECT.ProspectType;
import com.xpertcash.entity.PROSPECT.Prospect;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, Long>{
    Page<Prospect> findByCompanyNameContainingIgnoreCase(String q, Pageable pageable);
    Optional<Prospect> findByEmail(String email);
    
    // Méthodes avec filtrage par entreprise
    Page<Prospect> findByEntrepriseId(Long entrepriseId, Pageable pageable);
    Page<Prospect> findByEntrepriseIdAndCompanyNameContainingIgnoreCase(Long entrepriseId, String q, Pageable pageable);
    Optional<Prospect> findByIdAndEntrepriseId(Long id, Long entrepriseId);
    Optional<Prospect> findByEmailAndEntrepriseId(String email, Long entrepriseId);
    
    // Méthodes avec filtrage par type
    Page<Prospect> findByEntrepriseIdAndType(Long entrepriseId, ProspectType type, Pageable pageable);
    
    // Recherche avancée par nom/prénom selon le type
    Page<Prospect> findByEntrepriseIdAndTypeAndCompanyNameContainingIgnoreCase(Long entrepriseId, ProspectType type, String q, Pageable pageable);
    Page<Prospect> findByEntrepriseIdAndTypeAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(Long entrepriseId, ProspectType type, String firstName, String lastName, Pageable pageable);

}
