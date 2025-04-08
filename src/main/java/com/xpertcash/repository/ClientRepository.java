package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    List<Client> findByEntrepriseClientId(Long entrepriseClientId);
    Optional<Client> findByEmailOrTelephone(String email, String telephone);

}
