package com.xpertcash.repository;

import com.xpertcash.entity.CategoryProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryProduitRepository extends JpaRepository<CategoryProduit, Long> {
    Optional<CategoryProduit> findByNomCategory(String nomCategory);
    Optional<CategoryProduit> findById(Long id);
}
