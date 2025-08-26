package com.xpertcash.repository.VENTE;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteHistorique;

public interface VenteHistoriqueRepository extends JpaRepository<VenteHistorique, Long> {
    List<VenteHistorique> findByVenteId(Long venteId);

     List<VenteHistorique> findByVenteAndAction(Vente vente, String action);
     
}