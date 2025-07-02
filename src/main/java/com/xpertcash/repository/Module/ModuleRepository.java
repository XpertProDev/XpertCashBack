package com.xpertcash.repository.Module;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Module.AppModule;

@Repository
public interface ModuleRepository extends JpaRepository<AppModule, Long> {
    boolean existsByNom(String nom);
    List<AppModule> findByActifParDefautTrue();
   Optional<AppModule> findByNom(String nom);
     Optional<AppModule> findByCode(String code);
     boolean existsByCode(String code);
    List<AppModule> findByPayantTrue();


}
