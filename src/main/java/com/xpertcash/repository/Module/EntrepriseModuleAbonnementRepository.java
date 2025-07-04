package com.xpertcash.repository.Module;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.entity.Module.EntrepriseModuleAbonnement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Repository
public interface EntrepriseModuleAbonnementRepository extends JpaRepository<EntrepriseModuleAbonnement, Long> {

    Optional<EntrepriseModuleAbonnement> findByEntrepriseAndModule_CodeAndActifTrue(Entreprise entreprise, String codeModule);

    boolean existsByEntrepriseAndModuleAndActifTrue(Entreprise entreprise, AppModule module);

    List<EntrepriseModuleAbonnement> findByEntrepriseAndActifTrue(Entreprise entreprise);

    Page<EntrepriseModuleAbonnement> findByDateFinBeforeAndActifTrue(LocalDateTime date, Pageable pageable);

    Optional<EntrepriseModuleAbonnement> findByEntrepriseAndModuleAndActifTrue(Entreprise entreprise, AppModule module);

    Optional<EntrepriseModuleAbonnement> findTopByEntrepriseAndModuleOrderByDateFinDesc(Entreprise entreprise, AppModule module);

    List<EntrepriseModuleAbonnement> findByActifTrueAndDateFinBefore(LocalDateTime date);





}


