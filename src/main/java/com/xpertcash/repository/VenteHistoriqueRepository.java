package com.xpertcash.repository;

import com.xpertcash.entity.VenteHistorique;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VenteHistoriqueRepository extends JpaRepository<VenteHistorique, Long> {
    List<VenteHistorique> findByVenteId(Long venteId);
}