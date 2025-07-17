package com.xpertcash.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.StatutPaiementFacture;

@Repository
public interface FactureReelleRepository extends JpaRepository<FactureReelle, Long> {

    Optional<FactureReelle> findTopByDateCreationOrderByNumeroFactureDesc(LocalDate dateCreation);

    @Query("SELECT f FROM FactureReelle f WHERE FUNCTION('YEAR', f.dateCreation) = :year ORDER BY f.numeroFacture DESC")
    List<FactureReelle> findFacturesDeLAnnee(@Param("year") int year);
    

    List<FactureReelle> findByEntreprise(Entreprise entreprise);

    // Trie

    @Query("SELECT f FROM FactureReelle f WHERE MONTH(f.dateCreation) = :mois AND YEAR(f.dateCreation) = :annee AND f.entreprise.id = :entrepriseId")
    List<FactureReelle> findByMonthAndYearAndEntreprise(@Param("mois") Integer mois, @Param("annee") Integer annee, @Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT f FROM FactureReelle f WHERE MONTH(f.dateCreation) = :mois AND f.entreprise.id = :entrepriseId")
    List<FactureReelle> findByMonthAndEntreprise(@Param("mois") Integer mois, @Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT f FROM FactureReelle f WHERE YEAR(f.dateCreation) = :annee AND f.entreprise.id = :entrepriseId")
    List<FactureReelle> findByYearAndEntreprise(@Param("annee") Integer annee, @Param("entrepriseId") Long entrepriseId);
    
    List<FactureReelle> findByEntrepriseId(Long entrepriseId);


   Optional<FactureReelle> findByFactureProForma(FactureProForma factureProForma);

    @Query("SELECT fr FROM FactureReelle fr WHERE fr.entreprise = :entreprise ORDER BY fr.dateCreation DESC, fr.id DESC")
    List<FactureReelle> findByEntrepriseOrderByDateCreationDesc(@Param("entreprise") Entreprise entreprise);


    List<FactureReelle> findByEntrepriseIdAndStatutPaiementIn(Long entrepriseId, List<StatutPaiementFacture> statuts);

    List<FactureReelle> findByEntrepriseIdAndUtilisateurCreateurIdAndStatutPaiementIn(
        Long entrepriseId, Long utilisateurId, List<StatutPaiementFacture> statuts
    );

    List<FactureReelle> findAllByFactureProForma(FactureProForma factureProForma);

    List<FactureReelle> findByEntrepriseAndUtilisateurCreateurOrderByDateCreationDesc(Entreprise entreprise, User utilisateurCreateur);

    boolean existsByClientId(Long clientId);
    boolean existsByEntrepriseClientId(Long entrepriseClientId);





}

