package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    List<Boutique> findByEntrepriseId(Long entrepriseId);
}

