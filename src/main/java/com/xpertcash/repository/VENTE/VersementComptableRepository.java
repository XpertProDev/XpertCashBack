package com.xpertcash.repository.VENTE;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.VENTE.StatutVersement;
import com.xpertcash.entity.VENTE.VersementComptable;

public interface VersementComptableRepository extends JpaRepository<VersementComptable, Long>{
    List<VersementComptable> findByCaisse_BoutiqueId(Long boutiqueId);
    List<VersementComptable> findByCaisse_BoutiqueIdAndStatut(Long boutiqueId, StatutVersement statut);



}
