package com.xpertcash.repository.VENTE;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.VENTE.VenteProduit;

public interface VenteProduitRepository extends JpaRepository<VenteProduit, Long> {

    Optional<VenteProduit> findByVenteIdAndProduitId(Long venteId, Long produitId);
}