package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.FactureProForma;

@Repository
public interface FactureProformaRepository extends JpaRepository<FactureProForma, Long> {
     @Query("SELECT f FROM FactureProForma f LEFT JOIN FETCH f.client WHERE f.id = :id")
    Optional<FactureProForma> findByIdWithClient(@Param("id") Long id);
}

