package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.LigneFactureReelle;

@Repository

public interface LigneFactureReelleRepository extends JpaRepository<LigneFactureReelle, Long>{

}
