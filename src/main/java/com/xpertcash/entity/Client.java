package com.xpertcash.entity;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
    @Index(name = "idx_client_entreprise_id", columnList = "entreprise_id"),
    @Index(name = "idx_client_nom_complet", columnList = "nom_complet"),
    @Index(name = "idx_client_entreprise_client_id", columnList = "entreprise_client_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomComplet;
    private String adresse;
    private String poste;
    private String pays;
    private String ville;
    private String telephone;
    private String email;
    @Column(nullable = true)
    private String photo;
    private LocalDateTime createdAt;
    
    /**
     * Indique si ce client provient automatiquement d'un employé (User).
     * null ou false = client normal créé manuellement.
     * true = client lié à un employé.
     */
    @Column(name = "from_user", nullable = true)
    private Boolean fromUser;
    
   @ManyToOne
    @JoinColumn(name = "entreprise_client_id")
   @JsonIgnoreProperties("clientts")
    private EntrepriseClient entrepriseClient;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"facturesProforma", "identifiantEntreprise", "utilisateurs", "adresse", "boutiques", "createdAt", "logo", "admin"})
    private Entreprise entreprise;

    /** Présence d'écart caisse à son compte (rempli côté service pour le détail client). Non persisté. */
    @Transient
    private Boolean hasEcart;
    /** Nombre d'écarts caisse non soldés (rempli côté service). Non persisté. */
    @Transient
    private Integer nombreEcarts;
    /** Montant restant à régler au titre des écarts caisse, après prise en compte des paiements (rempli côté service). Non persisté. */
    @Transient
    private Double montantEcartRestant;

}
