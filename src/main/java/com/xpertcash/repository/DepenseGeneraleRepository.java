package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.DepenseGenerale;

@Repository
public interface DepenseGeneraleRepository extends JpaRepository<DepenseGenerale, Long> {
    List<DepenseGenerale> findByEntrepriseId(Long entrepriseId);
    
    List<DepenseGenerale> findByEntrepriseIdOrderByDateCreationDesc(Long entrepriseId);
}

