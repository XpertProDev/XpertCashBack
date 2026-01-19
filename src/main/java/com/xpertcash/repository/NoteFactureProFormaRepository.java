package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.NoteFactureProForma;

@Repository
public interface NoteFactureProFormaRepository extends JpaRepository<NoteFactureProForma, Long>{
    
    // Recherche par facture ID et entreprise (pour isolation, optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT n FROM NoteFactureProForma n " +
           "LEFT JOIN FETCH n.facture f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH n.auteur a " +
           "WHERE f.id = :factureProFormaId AND e.id = :entrepriseId " +
           "ORDER BY n.dateCreation ASC")
    List<NoteFactureProForma> findByFactureProFormaIdAndEntrepriseId(
            @Param("factureProFormaId") Long factureProFormaId,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par ID de note et entreprise (pour isolation)
    @Query("SELECT DISTINCT n FROM NoteFactureProForma n " +
           "LEFT JOIN FETCH n.facture f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH n.auteur a " +
           "WHERE n.id = :noteId AND e.id = :entrepriseId")
    Optional<NoteFactureProForma> findByIdAndEntrepriseId(
            @Param("noteId") Long noteId,
            @Param("entrepriseId") Long entrepriseId);

    // Suppression par facture ID et entreprise (pour isolation)
    @Modifying
    @Query("DELETE FROM NoteFactureProForma n " +
           "WHERE n.facture.id = :factureProFormaId " +
           "AND n.facture.entreprise.id = :entrepriseId")
    void deleteByFactureProFormaIdAndEntrepriseId(
            @Param("factureProFormaId") Long factureProFormaId,
            @Param("entrepriseId") Long entrepriseId);

}
