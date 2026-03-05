package com.xpertcash.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public AssistanceTicketDTO createTicket(User user, String objet, String message, MultipartFile pieceJointeFile) {
        if (user.getEntreprise() == null) {
            throw new BusinessException("Utilisateur sans entreprise : impossible de créer un ticket d'assistance.");
        }
        String pieceJointePath = null;
        if (pieceJointeFile != null && !pieceJointeFile.isEmpty()) {
            pieceJointePath = imageStorageService.saveSupportPieceJointe(pieceJointeFile);
        }
        AssistanceTicket ticket = new AssistanceTicket();
        ticket.setNumeroTicket(generateTicketNumber());
        // Sujet : priorité à l'objet fourni, sinon génération auto à partir du message
        String sujet;
        if (objet != null && !objet.isBlank()) {
            sujet = objet;
        } else if (message != null && !message.isBlank()) {
            sujet = message.length() > 80 ? message.substring(0, 80) + "..." : message;
        } else {
            sujet = "Ticket assistance";
        }
        ticket.setSujet(sujet);
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
        if (isSupport && (ticket.getStatut() == AssistanceStatus.EN_ATTENTE
                || ticket.getStatut() == AssistanceStatus.CONTESTE)) {
            ticket.setStatut(AssistanceStatus.EN_COURS);
        }
        // Si le ticket était RESOLU mais non validé, et que le client renvoie un message,
        // on le marque comme contesté (le support voit un statut différent)
        if (!isSupport && ticket.getStatut() == AssistanceStatus.RESOLU && !ticket.isValideParClient()) {
            ticket.setStatut(AssistanceStatus.CONTESTE);
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

    /** Rapport / statistiques pour le dashboard support (réservé SUPPORT ou SUPER_ADMIN). */
    public Map<String, Object> getSupportRapport(User supportUser) {
        ensureSupport(supportUser);
        long total = ticketRepository.countByDeletedFalse();
        long enAttente = ticketRepository.countByStatutAndDeletedFalse(AssistanceStatus.EN_ATTENTE);
        long enCours = ticketRepository.countByStatutAndDeletedFalse(AssistanceStatus.EN_COURS);
        long contestes = ticketRepository.countByStatutAndDeletedFalse(AssistanceStatus.CONTESTE);
        long resolueNonValidee = ticketRepository.countByStatutAndValideParClientAndDeletedFalse(AssistanceStatus.RESOLU, false);
        long resolueValidee = ticketRepository.countByStatutAndValideParClientAndDeletedFalse(AssistanceStatus.RESOLU, true);
        long nouveauxCetteSemaine = ticketRepository.countByCreatedAtAfterAndDeletedFalse(LocalDateTime.now().minusDays(7));
        long nouveauxCeMois = ticketRepository.countByCreatedAtAfterAndDeletedFalse(LocalDateTime.now().minusDays(30));

        Map<String, Object> rapport = new LinkedHashMap<>();
        rapport.put("totalTickets", total);
        rapport.put("enAttente", enAttente);
        rapport.put("enCours", enCours);
        rapport.put("contestes", contestes);
        rapport.put("resolusEnAttenteValidation", resolueNonValidee);
        rapport.put("resolusValides", resolueValidee);
        rapport.put("nouveauxCetteSemaine", nouveauxCetteSemaine);
        rapport.put("nouveauxCeMois", nouveauxCeMois);
        return rapport;
    }

    /**
     * Rapport historique par jour sur une période donnée.
     * periode = "SEMAINE" (7 jours) ou "MOIS" (30 jours).
     */
    public List<Map<String, Object>> getSupportRapportHistorique(User supportUser, String periode) {
        ensureSupport(supportUser);
        String p = (periode != null ? periode.toUpperCase() : "SEMAINE");
        int nbJours;
        if ("MOIS".equals(p)) {
            nbJours = 30;
        } else if ("SEMAINE".equals(p)) {
            nbJours = 7;
        } else {
            throw new BusinessException("Période invalide. Utiliser SEMAINE ou MOIS.");
        }

        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateDebut = aujourdHui.minusDays(nbJours - 1L);
        LocalDateTime startDateTime = dateDebut.atStartOfDay();
        LocalDateTime endDateTime = aujourdHui.plusDays(1L).atStartOfDay().minusNanos(1L);

        // Map date -> [nouveaux, enCours, resolus, contestes]
        Map<LocalDate, int[]> compteurParJour = new LinkedHashMap<>();
        LocalDate cursor = dateDebut;
        while (!cursor.isAfter(aujourdHui)) {
            compteurParJour.put(cursor, new int[] { 0, 0, 0, 0 });
            cursor = cursor.plusDays(1);
        }

        // Nouveaux tickets (créés dans la période)
        List<AssistanceTicket> crees = ticketRepository.findByCreatedAtBetweenAndDeletedFalse(startDateTime, endDateTime);
        for (AssistanceTicket t : crees) {
            if (t.getCreatedAt() != null) {
                LocalDate d = t.getCreatedAt().toLocalDate();
                int[] arr = compteurParJour.get(d);
                if (arr != null) {
                    arr[0] += 1;
                }
            }
        }

        // Tickets passés en EN_COURS ou CONTESTE (on se base sur updatedAt dans la période)
        List<AssistanceTicket> maj = ticketRepository.findByUpdatedAtBetweenAndDeletedFalse(startDateTime, endDateTime);
        for (AssistanceTicket t : maj) {
            if (t.getUpdatedAt() != null) {
                LocalDate d = t.getUpdatedAt().toLocalDate();
                int[] arr = compteurParJour.get(d);
                if (arr != null) {
                    if (t.getStatut() == AssistanceStatus.EN_COURS) {
                        arr[1] += 1;
                    } else if (t.getStatut() == AssistanceStatus.CONTESTE) {
                        arr[3] += 1;
                    }
                }
            }
        }

        // Tickets résolus (closedAt dans la période)
        List<AssistanceTicket> resolus = ticketRepository.findByClosedAtBetweenAndDeletedFalse(startDateTime, endDateTime);
        for (AssistanceTicket t : resolus) {
            if (t.getClosedAt() != null && t.getStatut() == AssistanceStatus.RESOLU) {
                LocalDate d = t.getClosedAt().toLocalDate();
                int[] arr = compteurParJour.get(d);
                if (arr != null) {
                    arr[2] += 1;
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        List<Map<String, Object>> resultat = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entry : compteurParJour.entrySet()) {
            LocalDate d = entry.getKey();
            int[] arr = entry.getValue();
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("date", d.format(formatter));
            ligne.put("nouveaux", arr[0]);
            ligne.put("enCours", arr[1]);
            ligne.put("resolus", arr[2]);
            ligne.put("contestes", arr[3]);
            resultat.add(ligne);
        }
        return resultat;
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

    /** Refus explicite de la résolution par le client (clic sur \"Non\"). */
    @Transactional
    public AssistanceTicketDTO refuserResolutionParClient(User user, Long ticketId) {
        AssistanceTicket ticket = loadTicketForUser(user, ticketId);
        if (ticket.getStatut() != AssistanceStatus.RESOLU) {
            throw new BusinessException("Le ticket doit être en statut RESOLU pour être refusé.");
        }
        ticket.setStatut(AssistanceStatus.CONTESTE);
        ticket.setValideParClient(false);
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
        String base = "Ticket-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8)
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

