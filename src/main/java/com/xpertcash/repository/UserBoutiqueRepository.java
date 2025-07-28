package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.UserBoutique;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBoutiqueRepository extends JpaRepository<UserBoutique, Long> {

    // Trouver toutes les affectations pour un utilisateur
    List<UserBoutique> findByUserId(Long userId);

    // Trouver une affectation spécifique par utilisateur et boutique
    Optional<UserBoutique> findByUserIdAndBoutiqueId(Long userId, Long boutiqueId);

    // Trouver toutes les affectations pour une boutique donnée
    List<UserBoutique> findByBoutiqueId(Long boutiqueId);

    // Supprimer toutes les affectations d'un utilisateur donné
    void deleteByUserId(Long userId);

    // Supprimer l'affectation d'un utilisateur pour une boutique spécifique
    void deleteByUserIdAndBoutiqueId(Long userId, Long boutiqueId);
}
