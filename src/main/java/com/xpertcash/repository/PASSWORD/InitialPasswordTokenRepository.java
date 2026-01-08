package com.xpertcash.repository.PASSWORD;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.User;
import com.xpertcash.entity.PASSWORD.InitialPasswordToken;

@Repository
public interface InitialPasswordTokenRepository extends JpaRepository<InitialPasswordToken, Long> {

    Optional<InitialPasswordToken> findByUser(User user);

    Optional<InitialPasswordToken> findByToken(String token);

    void deleteByUser(User user);

}

