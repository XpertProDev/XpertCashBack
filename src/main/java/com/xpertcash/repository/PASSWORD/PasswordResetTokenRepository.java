package com.xpertcash.repository.PASSWORD;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.User;
import com.xpertcash.entity.PASSWORD.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUserAndToken(User user, String token);

    Optional<PasswordResetToken> findByUser(User user);

    void deleteByUser(User user);

    Optional<PasswordResetToken> findByToken(String token);

}
