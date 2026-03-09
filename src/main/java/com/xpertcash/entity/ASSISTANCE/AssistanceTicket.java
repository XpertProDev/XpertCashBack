package com.xpertcash.entity.ASSISTANCE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistance_ticket")
public class AssistanceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ticket", nullable = false, unique = true, length = 50)
    private String numeroTicket;

    @Column(nullable = false, length = 255)
    private String sujet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssistanceStatus statut = AssistanceStatus.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /** Indique si le client a confirmé que la solution du support règle bien son problème. */
    @Column(name = "valide_par_client", nullable = false)
    private boolean valideParClient = false;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssistanceMessage> messages = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getNumeroTicket() {
        return numeroTicket;
    }

    public void setNumeroTicket(String numeroTicket) {
        this.numeroTicket = numeroTicket;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public AssistanceStatus getStatut() {
        return statut;
    }

    public void setStatut(AssistanceStatus statut) {
        this.statut = statut;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isValideParClient() {
        return valideParClient;
    }

    public void setValideParClient(boolean valideParClient) {
        this.valideParClient = valideParClient;
    }

    public List<AssistanceMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AssistanceMessage> messages) {
        this.messages = messages;
    }
}

