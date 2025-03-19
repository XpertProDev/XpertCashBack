package com.xpertcash.repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    
    // Méthode pour trouver un utilisateur par email et entreprise
    Optional<User> findByEmailAndEntreprise(String email, Entreprise entreprise);
    
    // Méthode pour trouver un utilisateur par téléphone et entreprise
    Optional<User> findByPhoneAndEntreprise(String phone, Entreprise entreprise);
    Optional<User> findByPhoneAndEntrepriseAndPays(String phone, Entreprise entreprise, String pays);
    List<User> findByEntreprise(Entreprise entreprise);

    // Méthode pour récupérer tous les utilisateurs d'une entreprise
    List<User> findByEntrepriseId(Long entrepriseId);
}

