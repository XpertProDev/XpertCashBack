package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    List<Client> findByEntrepriseClientId(Long entrepriseClientId);

}
