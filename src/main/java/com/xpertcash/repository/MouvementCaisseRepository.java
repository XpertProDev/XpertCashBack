package com.xpertcash.repository;

import com.xpertcash.entity.MouvementCaisse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MouvementCaisseRepository extends JpaRepository<MouvementCaisse, Long> {
}