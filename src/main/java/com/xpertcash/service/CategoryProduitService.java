package com.xpertcash.service;

import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.repository.CategoryProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryProduitService {

    @Autowired
    private CategoryProduitRepository categoryProduitRepository;

    //Crée une nouvelle catégorie.
    public CategoryProduit createCategory(CategoryProduit category) {
        if (category.getNomCategory() == null || category.getNomCategory().trim().isEmpty()) {
            throw new RuntimeException("Le nom de la catégorie est obligatoire");
        }
        // Vérification d'unicité sur le nom
        Optional<CategoryProduit> existing = categoryProduitRepository.findByNomCategory(category.getNomCategory());
        if(existing.isPresent()){
            throw new RuntimeException("La catégorie existe déjà");
        }
        return categoryProduitRepository.save(category);
    }

    // Met à jour une catégorie existante.
    public CategoryProduit updateCategory(Long id, CategoryProduit updatedCategory) {
        CategoryProduit category = getCategoryById(id);
        if (updatedCategory.getNomCategory() == null || updatedCategory.getNomCategory().trim().isEmpty()) {
            throw new RuntimeException("Le nom de la catégorie est obligatoire");
        }
        if (!category.getNomCategory().equals(updatedCategory.getNomCategory())) {
            Optional<CategoryProduit> duplicate = categoryProduitRepository.findByNomCategory(updatedCategory.getNomCategory());
            if (duplicate.isPresent()) {
                throw new RuntimeException("La catégorie existe déjà");
            }
        }
        category.setNomCategory(updatedCategory.getNomCategory());
        return categoryProduitRepository.save(category);
    }

    // Suppression de catégorie existante.
    public void deleteCategory(Long id) {
        CategoryProduit category = getCategoryById(id);
        categoryProduitRepository.delete(category);
    }

    // Listes des catégories existante.
    public List<CategoryProduit> getAllCategories() {
        return categoryProduitRepository.findAll();
    }

    // Listes des catégories existante par ID avect les produit qui est lier
    @Transactional
    public CategoryProduit getCategoryById(Long id) {
        return categoryProduitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id : " + id));
    }

}
