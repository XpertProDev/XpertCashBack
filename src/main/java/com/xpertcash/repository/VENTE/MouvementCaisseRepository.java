package com.xpertcash.repository.VENTE;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.xpertcash.entity.VENTE.MouvementCaisse;
import com.xpertcash.entity.VENTE.TypeMouvementCaisse;

public interface MouvementCaisseRepository extends JpaRepository<MouvementCaisse, Long> {
    
    List<MouvementCaisse> findByCaisseIdAndTypeMouvement(Long caisseId, TypeMouvementCaisse typeMouvement);
}