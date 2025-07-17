package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Facture;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long>{
   List<Facture> findByBoutique(Boutique boutique);

   @Query("SELECT f FROM Facture f WHERE f.boutique.id = :boutiqueId")
   List<Facture> findByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

  
    List<Facture> findByFournisseur_Id(Long fournisseurId);

    @Query("SELECT f FROM Facture f WHERE YEAR(f.dateFacture) = :year")
   List<Facture> findByYear(@Param("year") int year);

   boolean existsByFournisseur_Id(Long fournisseurId);
 

}
