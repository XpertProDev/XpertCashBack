package com.xpertcash.entity;

import com.xpertcash.entity.Enum.SourceTresorerie;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transfert_fonds")
public class TransfertFonds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "source")
    private SourceTresorerie source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "destination")
    private SourceTresorerie destination;

    @Column(nullable = false, length = 500)
    private String motif;

    @Column(nullable = false, length = 500)
    private String personneALivrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id", nullable = false)
    private User creePar;

    @Column(nullable = false)
    private LocalDateTime dateTransfert;

    @PrePersist
    public void prePersist() {
        if (dateTransfert == null) {
            dateTransfert = LocalDateTime.now();
        }
    }
}

