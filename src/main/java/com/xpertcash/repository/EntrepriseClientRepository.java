package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;

@Repository
public interface EntrepriseClientRepository extends JpaRepository<EntrepriseClient, Long>{
    Optional<EntrepriseClient> findByNom(String nom);
    Optional<EntrepriseClient> findByEmailOrTelephone(String email, String telephone);
    Optional<EntrepriseClient> findByEmail(String email);
    Optional<EntrepriseClient> findByTelephone(String telephone);



}
