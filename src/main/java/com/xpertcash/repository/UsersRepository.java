package com.xpertcash.repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    
    // Méthode pour trouver un utilisateur par email et entreprise
    Optional<User> findByEmailAndEntreprise(String email, Entreprise entreprise);
    
    // Méthode pour trouver un utilisateur par téléphone et entreprise
    Optional<User> findByPhoneAndEntreprise(String phone, Entreprise entreprise);
    Optional<User> findByPhoneAndEntrepriseAndPays(String phone, Entreprise entreprise, String pays);
    List<User> findByEntreprise(Entreprise entreprise);

    // Méthode pour récupérer tous les utilisateurs d'une entreprise
    List<User> findByEntrepriseId(Long entrepriseId);

    // Méthode pour récupérer tous les utilisateurs d'une entreprise sauf l'ADMIN
    List<User> findByEntrepriseIdAndIdNot(Long entrepriseId, Long adminId);
    

    boolean existsByPersonalCode(String personalCode);
    //List<User> findByBoutiqueIdAndRole_Name(Long boutiqueId, RoleType roleName);

    Optional<User> findByEntrepriseIdAndRole_NameIn(Long entrepriseId, List<RoleType> roles);

   
     @Query("SELECT COUNT(u) FROM User u WHERE u.entreprise.id = :entrepriseId AND u.role.name <> :excludedRole")
    long countByEntrepriseIdExcludingRole(@Param("entrepriseId") Long entrepriseId,
                                          @Param("excludedRole") RoleType excludedRole);



// Récupérer l'utilisateur avec entreprise et role en une seule requête
@Query("SELECT u FROM User u " +
       "JOIN FETCH u.entreprise e " +
       "JOIN FETCH u.role r " +
       "LEFT JOIN FETCH r.permissions p " +
       "WHERE u.id = :userId")
Optional<User> findByIdWithEntrepriseAndRole(@Param("userId") Long userId);




}

