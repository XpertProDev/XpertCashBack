package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.AssistanceTicketDTO;
import com.xpertcash.DTOs.AssistanceMessageDTO;
import com.xpertcash.entity.ASSISTANCE.AssistanceMessage;
import com.xpertcash.entity.ASSISTANCE.AssistanceStatus;
import com.xpertcash.entity.ASSISTANCE.AssistanceTicket;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.BusinessException;
import com.xpertcash.repository.ASSISTANCE.AssistanceMessageRepository;
import com.xpertcash.repository.ASSISTANCE.AssistanceTicketRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

@Service
public class AssistanceService {

    @Autowired
    private AssistanceTicketRepository ticketRepository;

    @Autowired
    private AssistanceMessageRepository messageRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @Transactional
    public AssistanceTicketDTO createTicket(User user, String message, MultipartFile pieceJointeFile) {
        if (user.getEntreprise() == null) {
            throw new BusinessException("Utilisateur sans entreprise : impossible de créer un ticket d'assistance.");
        }
        String pieceJointePath = null;
        if (pieceJointeFile != null && !pieceJointeFile.isEmpty()) {
            pieceJointePath = imageStorageService.saveSupportPieceJointe(pieceJointeFile);
        }
        AssistanceTicket ticket = new AssistanceTicket();
        ticket.setNumeroTicket(generateTicketNumber());
        // Génère un sujet automatique à partir du message
        String autoSujet = (message != null && !message.isBlank())
                ? (message.length() > 80 ? message.substring(0, 80) + "..." : message)
                : "Ticket assistance";
        ticket.setSujet(autoSujet);
        ticket.setStatut(AssistanceStatus.EN_ATTENTE);
        ticket.setCreatedBy(user);
        ticket.setEntreprise(user.getEntreprise());
        ticket.setCreatedAt(LocalDateTime.now());

        AssistanceMessage first = new AssistanceMessage();
        first.setTicket(ticket);
        first.setAuteur(user);
        first.setContenu(message);
        first.setPieceJointePath(pieceJointePath);
        first.setSupport(false);
        first.setCreatedAt(LocalDateTime.now());

        ticket.getMessages().add(first);

        AssistanceTicket saved = ticketRepository.save(ticket);
        return toTicketDTO(saved, true);
    }

    public List<AssistanceTicketDTO> getMyTickets(User user) {
        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        if (entrepriseId == null) {
            throw new BusinessException("Utilisateur sans entreprise.");
        }
        return ticketRepository
                .findByEntreprise_IdAndCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(entrepriseId, user.getId())
                .stream()
                .map(t -> toTicketDTO(t, false))
                .collect(Collectors.toList());
    }

    public List<AssistanceMessageDTO> getMessagesForTicket(User user, Long ticketId) {
        AssistanceTicket ticket = loadTicketForUser(user, ticketId);
        return messageRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssistanceMessageDTO addMessage(User user, Long ticketId, String contenu, MultipartFile pieceJointeFile) {
        AssistanceTicket ticket = loadTicketForUser(user, ticketId);
        // Si le ticket est déjà validé par le client, on refuse tout nouveau message
        if (ticket.getStatut() == AssistanceStatus.RESOLU && ticket.isValideParClient()) {
            throw new BusinessException(
                    "Ce ticket est déjà résolu et validé. Merci de créer un nouveau ticket pour un autre problème.");
        }
        boolean isSupport = isSupportUser(user);
        String pieceJointePath = null;
        if (pieceJointeFile != null && !pieceJointeFile.isEmpty()) {
            pieceJointePath = imageStorageService.saveSupportPieceJointe(pieceJointeFile);
        }

        AssistanceMessage msg = new AssistanceMessage();
        msg.setTicket(ticket);
        msg.setAuteur(user);
        msg.setContenu(contenu);
        msg.setPieceJointePath(pieceJointePath);
        msg.setSupport(isSupport);
        msg.setCreatedAt(LocalDateTime.now());

        messageRepository.save(msg);

        ticket.setUpdatedAt(LocalDateTime.now());
        if (isSupport && ticket.getStatut() == AssistanceStatus.EN_ATTENTE) {
            ticket.setStatut(AssistanceStatus.EN_COURS);
        }
        // Si le ticket était RESOLU mais non validé, et que le client renvoie un message,
        // on le rouvre en EN_COURS
        if (!isSupport && ticket.getStatut() == AssistanceStatus.RESOLU && !ticket.isValideParClient()) {
            ticket.setStatut(AssistanceStatus.EN_COURS);
        }
        ticketRepository.save(ticket);

        return toMessageDTO(msg);
    }

    public List<AssistanceTicketDTO> supportListTickets(User supportUser, AssistanceStatus status) {
        ensureSupport(supportUser);
        List<AssistanceTicket> tickets;
        if (status != null) {
            tickets = ticketRepository.findByStatutAndDeletedFalseOrderByCreatedAtDesc(status);
        } else {
            tickets = ticketRepository.findByDeletedFalseOrderByCreatedAtDesc();
        }
        return tickets.stream().map(t -> toTicketDTO(t, false)).collect(Collectors.toList());
    }

    @Transactional
    public AssistanceTicketDTO changeStatus(User supportUser, Long ticketId, AssistanceStatus newStatus) {
        ensureSupport(supportUser);
        AssistanceTicket ticket = ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new BusinessException("Ticket introuvable."));
        ticket.setStatut(newStatus);
        if (newStatus == AssistanceStatus.RESOLU) {
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setValideParClient(false);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        return toTicketDTO(ticketRepository.save(ticket), false);
    }

    /** Validation finale par le client : confirme que la résolution est correcte. */
    @Transactional
    public AssistanceTicketDTO validerResolutionParClient(User user, Long ticketId) {
        AssistanceTicket ticket = loadTicketForUser(user, ticketId);
        if (ticket.getStatut() != AssistanceStatus.RESOLU) {
            throw new BusinessException("Le ticket doit être en statut RESOLU pour être validé.");
        }
        ticket.setValideParClient(true);
        ticket.setUpdatedAt(LocalDateTime.now());
        return toTicketDTO(ticketRepository.save(ticket), false);
    }

    @Transactional
    public void deleteTicket(User supportUser, Long ticketId) {
        ensureSupport(supportUser);
        AssistanceTicket ticket = ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new BusinessException("Ticket introuvable."));
        // Supprimer les fichiers des pièces jointes associées avant de supprimer définitivement le ticket
        List<AssistanceMessage> messages = messageRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        for (AssistanceMessage msg : messages) {
            if (msg.getPieceJointePath() != null && !msg.getPieceJointePath().isBlank()) {
                imageStorageService.deleteSupportPieceJointe(msg.getPieceJointePath());
            }
        }
        // La contrainte ON DELETE CASCADE sur assistance_message supprimera aussi tous les messages
        ticketRepository.delete(ticket);
    }

    private AssistanceTicket loadTicketForUser(User user, Long ticketId) {
        AssistanceTicket ticket = ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new BusinessException("Ticket introuvable."));
        boolean isSupport = isSupportUser(user);
        if (!isSupport && !user.getId().equals(ticket.getCreatedBy().getId())) {
            throw new BusinessException("Accès refusé au ticket.");
        }
        return ticket;
    }

    private boolean isSupportUser(User user) {
        return user.getRole() != null &&
                (user.getRole().getName() == RoleType.SUPPORT || user.getRole().getName() == RoleType.SUPER_ADMIN);
    }

    private void ensureSupport(User user) {
        if (!isSupportUser(user)) {
            throw new BusinessException("Accès réservé au support.");
        }
    }

    private String generateTicketNumber() {
        String base = "TCK-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8)
                .toUpperCase();
        if (!ticketRepository.existsByNumeroTicket(base)) {
            return base;
        }
        int suffix = 1;
        String candidate = base + "-" + suffix;
        while (ticketRepository.existsByNumeroTicket(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private AssistanceTicketDTO toTicketDTO(AssistanceTicket ticket, boolean includeMessages) {
        AssistanceTicketDTO dto = new AssistanceTicketDTO();
        dto.setId(ticket.getId());
        dto.setNumeroTicket(ticket.getNumeroTicket());
        dto.setSujet(ticket.getSujet());
        dto.setStatut(ticket.getStatut());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        dto.setClosedAt(ticket.getClosedAt());
        dto.setDeleted(ticket.isDeleted());
        dto.setValideParClient(ticket.isValideParClient());
        dto.setCreatedByNom(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getNomComplet() : null);
        dto.setCreatedByEmail(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getEmail() : null);
        if (includeMessages) {
            List<AssistanceMessageDTO> messages = ticket.getMessages().stream()
                    .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                    .map(this::toMessageDTO)
                    .collect(Collectors.toList());
            dto.setMessages(messages);
        }
        return dto;
    }

    private AssistanceMessageDTO toMessageDTO(AssistanceMessage msg) {
        AssistanceMessageDTO dto = new AssistanceMessageDTO();
        dto.setId(msg.getId());
        dto.setTicketId(msg.getTicket() != null ? msg.getTicket().getId() : null);
        dto.setAuteurId(msg.getAuteur() != null ? msg.getAuteur().getId() : null);
        dto.setAuteurNom(msg.getAuteur() != null ? msg.getAuteur().getNomComplet() : null);
        dto.setContenu(msg.getContenu());
        dto.setPieceJointePath(msg.getPieceJointePath());
        dto.setSupport(msg.isSupport());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }
}

