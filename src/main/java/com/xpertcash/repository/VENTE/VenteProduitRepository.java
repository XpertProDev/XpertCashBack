package com.xpertcash.repository.VENTE;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.VENTE.VenteProduit;

public interface VenteProduitRepository extends JpaRepository<VenteProduit, Long> {
}