package com.xpertcash.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.xpertcash.entity.EntrepriseClient;

@Repository
public interface EntrepriseClientRepository extends JpaRepository<EntrepriseClient, Long>{
    Optional<EntrepriseClient> findByNom(String nom);
    Optional<EntrepriseClient> findByEmailOrTelephone(String email, String telephone);
    Optional<EntrepriseClient> findByEmail(String email);
    Optional<EntrepriseClient> findByTelephone(String telephone);

    @Query("SELECT e FROM EntrepriseClient e WHERE e.email = :email AND e.entreprise.id = :entrepriseId")
    Optional<EntrepriseClient> findByEmailAndEntrepriseId(@Param("email") String email, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT e FROM EntrepriseClient e WHERE e.telephone = :telephone AND e.entreprise.id = :entrepriseId")
    Optional<EntrepriseClient> findByTelephoneAndEntrepriseId(@Param("telephone") String telephone, @Param("entrepriseId") Long entrepriseId);

     List<EntrepriseClient> findByEntrepriseId(Long entrepriseId);



}
