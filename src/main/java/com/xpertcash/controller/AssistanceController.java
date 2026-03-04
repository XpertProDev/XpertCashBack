package com.xpertcash.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.AssistanceMessageDTO;
import com.xpertcash.DTOs.AssistanceTicketDTO;
import com.xpertcash.entity.ASSISTANCE.AssistanceStatus;
import com.xpertcash.entity.User;
import com.xpertcash.service.AssistanceService;
import com.xpertcash.service.AuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AssistanceController {

    @Autowired
    private AssistanceService assistanceService;

    @Autowired
    private AuthenticationHelper authHelper;

    @PostMapping(path = "/tickets", consumes = {"multipart/form-data"})
    public ResponseEntity<?> creerTicket(
            @RequestParam("message") String message,
            @RequestParam(name = "pieceJointe", required = false) MultipartFile pieceJointe,
            HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            if (message == null || message.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Le champ 'message' est requis."));
            }
            AssistanceTicketDTO dto = assistanceService.createTicket(user, message, pieceJointe);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création du ticket : " + e.getMessage()));
        }
    }

    @GetMapping("/tickets")
    public ResponseEntity<?> listerMesTickets(HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            List<AssistanceTicketDTO> tickets = assistanceService.getMyTickets(user);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des tickets : " + e.getMessage()));
        }
    }

    @GetMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long ticketId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            List<AssistanceMessageDTO> messages = assistanceService.getMessagesForTicket(user, ticketId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des messages : " + e.getMessage()));
        }
    }

    @PostMapping(path = "/tickets/{ticketId}/messages", consumes = {"multipart/form-data"})
    public ResponseEntity<?> ajouterMessage(@PathVariable Long ticketId,
            @RequestParam("message") String contenu,
            @RequestParam(name = "pieceJointe", required = false) MultipartFile pieceJointe,
            HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            if (contenu == null || contenu.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Le champ 'message' est requis."));
            }
            AssistanceMessageDTO dto = assistanceService.addMessage(user, ticketId, contenu, pieceJointe);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'ajout du message : " + e.getMessage()));
        }
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<?> listerTicketsSupport(HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            AssistanceStatus status = null;
            if (body != null && body.get("status") != null) {
                status = AssistanceStatus.valueOf(body.get("status"));
            }
            List<AssistanceTicketDTO> tickets = assistanceService.supportListTickets(user, status);
            return ResponseEntity.ok(tickets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Status invalide."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des tickets support : " + e.getMessage()));
        }
    }

    @PatchMapping("/admin/tickets/{ticketId}/status")
    public ResponseEntity<?> changerStatut(@PathVariable Long ticketId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Le champ 'status' est requis."));
            }
            AssistanceStatus status = AssistanceStatus.valueOf(statusStr);
            AssistanceTicketDTO dto = assistanceService.changeStatus(user, ticketId, status);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Status invalide."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du changement de statut : " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/tickets/{ticketId}")
    public ResponseEntity<?> supprimerTicket(@PathVariable Long ticketId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            assistanceService.deleteTicket(user, ticketId);
            return ResponseEntity.ok(Map.of("message", "Ticket supprimé (archivé) avec succès."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression du ticket : " + e.getMessage()));
        }
    }

    /** Validation finale d'un ticket par le client (confirme que la solution est OK). */
    @PatchMapping("/tickets/{ticketId}/valider")
    public ResponseEntity<?> validerTicketParClient(@PathVariable Long ticketId, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            AssistanceTicketDTO dto = assistanceService.validerResolutionParClient(user, ticketId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Erreur lors de la validation du ticket : " + e.getMessage()));
        }
    }
}

