package com.xpertcash.repository.ASSISTANCE;

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
}

