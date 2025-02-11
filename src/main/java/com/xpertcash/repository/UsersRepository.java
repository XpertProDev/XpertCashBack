package com.xpertcash.repository;

import com.xpertcash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    //users findByEmail(String email);
    Optional<User> findByEmail(String email);

    //users findByEmailAndMotDePasse(String email, String mot_de_passe);
    //users findByUtilisateurId(Long id);
}
