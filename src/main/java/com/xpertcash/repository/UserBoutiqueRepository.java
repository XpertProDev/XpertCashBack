package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.UserBoutique;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBoutiqueRepository extends JpaRepository<UserBoutique, Long> {

    // Trouver toutes les affectations pour un utilisateur (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT ub FROM UserBoutique ub " +
           "LEFT JOIN FETCH ub.user u " +
           "LEFT JOIN FETCH ub.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE ub.user.id = :userId")
    List<UserBoutique> findByUserId(@Param("userId") Long userId);

    // Trouver une affectation spécifique par utilisateur et boutique (optimisé avec JOIN FETCH)
    @Query("SELECT ub FROM UserBoutique ub " +
           "LEFT JOIN FETCH ub.user u " +
           "LEFT JOIN FETCH ub.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE ub.user.id = :userId AND ub.boutique.id = :boutiqueId")
    Optional<UserBoutique> findByUserIdAndBoutiqueId(@Param("userId") Long userId, @Param("boutiqueId") Long boutiqueId);

    // Trouver toutes les affectations pour une boutique donnée (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT ub FROM UserBoutique ub " +
           "LEFT JOIN FETCH ub.user u " +
           "LEFT JOIN FETCH u.entreprise eu " +
           "LEFT JOIN FETCH u.role r " +
           "LEFT JOIN FETCH ub.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE ub.boutique.id = :boutiqueId")
    List<UserBoutique> findByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

    // Trouver toutes les affectations pour une entreprise (isolation et optimisation)
    @Query("SELECT DISTINCT ub FROM UserBoutique ub " +
           "LEFT JOIN FETCH ub.user u " +
           "LEFT JOIN FETCH u.entreprise eu " +
           "LEFT JOIN FETCH ub.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId OR eu.id = :entrepriseId")
    List<UserBoutique> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Supprimer toutes les affectations d'un utilisateur donné
    void deleteByUserId(Long userId);

    // Supprimer l'affectation d'un utilisateur pour une boutique spécifique
    void deleteByUserIdAndBoutiqueId(Long userId, Long boutiqueId);
}
