package com.xpertcash.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Fournisseur;


@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long>{


    Optional<Fournisseur> findByEmailOrTelephone(String email, String telephone);
    Optional<Fournisseur> findByEmail(String email);
    Optional<Fournisseur> findByTelephone(String telephone);
    List<Fournisseur> findByEntreprise(Entreprise entreprise);
    Optional<Fournisseur> findByNomComplet(String nomComplet);

    boolean existsByEntrepriseIdAndEmailAndIdNot(Long entrepriseId, String email, Long id);
    boolean existsByEntrepriseIdAndPaysAndTelephoneAndIdNot(Long entrepriseId, String pays, String telephone, Long id);




}
