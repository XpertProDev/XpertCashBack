package com.xpertcash.repository.ASSISTANCE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.ASSISTANCE.AssistanceStatus;
import com.xpertcash.entity.ASSISTANCE.AssistanceTicket;

@Repository
public interface AssistanceTicketRepository extends JpaRepository<AssistanceTicket, Long> {

    boolean existsByNumeroTicket(String numeroTicket);

    Optional<AssistanceTicket> findByIdAndDeletedFalse(Long id);

    List<AssistanceTicket> findByEntreprise_IdAndCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(Long entrepriseId, Long userId);

    List<AssistanceTicket> findByEntreprise_IdAndDeletedFalseOrderByCreatedAtDesc(Long entrepriseId);

    List<AssistanceTicket> findByEntreprise_IdAndStatutAndDeletedFalseOrderByCreatedAtDesc(Long entrepriseId, AssistanceStatus statut);

    // Vue globale pour le support (tous les tickets, toutes entreprises)
    List<AssistanceTicket> findByDeletedFalseOrderByCreatedAtDesc();

    List<AssistanceTicket> findByStatutAndDeletedFalseOrderByCreatedAtDesc(AssistanceStatus statut);

    // Rapports / statistiques support (totaux)
    long countByDeletedFalse();

    long countByStatutAndDeletedFalse(AssistanceStatus statut);

    long countByStatutAndValideParClientAndDeletedFalse(AssistanceStatus statut, boolean valideParClient);

    long countByCreatedAtAfterAndDeletedFalse(LocalDateTime after);

    // Rapports historiques (plage de dates)
    List<AssistanceTicket> findByCreatedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);

    List<AssistanceTicket> findByUpdatedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);

    List<AssistanceTicket> findByClosedAtBetweenAndDeletedFalse(LocalDateTime start, LocalDateTime end);
}

