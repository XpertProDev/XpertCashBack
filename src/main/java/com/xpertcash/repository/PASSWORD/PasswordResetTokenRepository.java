package com.xpertcash.repository.PASSWORD;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.PASSWORD.PasswordResetToken;

@Repository

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>{

    Optional<PasswordResetToken> findByEmailAndToken(String email, String token);
    
    Optional<PasswordResetToken> findByEmail(String email);

    void deleteByEmail(String email);



}
