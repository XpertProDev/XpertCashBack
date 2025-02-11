package com.xpertcash.repository;

import com.xpertcash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    //users findByEmailAndMotDePasse(String email, String mot_de_passe);
    //users findByUtilisateurId(Long id);
}
