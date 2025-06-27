package com.xpertcash.repository.Module;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Module.PaiementModule;

@Repository
public interface PaiementModuleRepository extends JpaRepository<PaiementModule, Long>{

     List<PaiementModule> findByEntrepriseId(Long entrepriseId);
}
