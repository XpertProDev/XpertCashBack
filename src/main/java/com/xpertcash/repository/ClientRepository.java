package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    List<Client> findByEntrepriseClientId(Long entrepriseClientId);
    Optional<Client> findByEmailOrTelephone(String email, String telephone);
    Optional<Client> findByEmail(String email);
    Optional<Client> findByTelephone(String telephone);

    @Query("SELECT c FROM Client c WHERE c.entreprise.id = :entrepriseId OR c.entrepriseClient.entreprise.id = :entrepriseId")
    List<Client> findClientsByEntrepriseOrEntrepriseClient(@Param("entrepriseId") Long entrepriseId);

    

}
