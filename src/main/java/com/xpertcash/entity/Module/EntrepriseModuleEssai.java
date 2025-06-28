package com.xpertcash.entity.Module;
import java.time.LocalDateTime;

import com.xpertcash.entity.Entreprise;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntrepriseModuleEssai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private AppModule module;

    private LocalDateTime dateDebutEssai;
    private LocalDateTime dateFinEssai;

}
