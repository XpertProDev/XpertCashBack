package com.xpertcash.controller;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/auth/test")
public class MailTestController {

    private static final Logger logger = LoggerFactory.getLogger(MailTestController.class);

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.facture.username}")
    private String factureUsername;

    @Value("${spring.mail.facture.password}")
    private String facturePassword;

    @PostMapping("/mail/connection")
    public ResponseEntity<Map<String, Object>> testMailConnection(
            @RequestParam(required = false) String testEmail) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Utiliser l'email de test fourni ou un email par défaut
        String recipientEmail = testEmail != null && !testEmail.isEmpty() 
            ? testEmail 
            : "carterhedy57@gmail.com";
        
        logger.info("🧪 Test de connexion SMTP - Host: {}, Port: {}, User: {}, Recipient: {}", 
            host, port, username, recipientEmail);
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
            
            // Désactiver STARTTLS pour le port 465
            props.put("mail.smtp.starttls.enable", "false");
            
            logger.debug("Propriétés SMTP configurées: {}", props);
            
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    logger.debug("Authentification demandée pour: {}", username);
                    return new PasswordAuthentication(username, password);
                }
            });
            
            logger.info("Session créée avec succès");
            
            // Test de connexion sans envoyer d'email
            Transport transport = session.getTransport("smtp");
            logger.info("Tentative de connexion au serveur SMTP...");
            transport.connect(host, port, username, password);
            logger.info("✅ Connexion SMTP réussie !");
            transport.close();
            
            // Si la connexion réussit, tester l'envoi d'un email
            logger.info("Tentative d'envoi d'un email de test...");
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "Tchakeda"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Test SMTP - Tchakeda");
            message.setText("Ceci est un email de test pour vérifier la configuration SMTP.\n\n" +
                          "Si vous recevez cet email, la configuration est correcte !");
            
            Transport.send(message);
            logger.info("✅ Email de test envoyé avec succès à {}", recipientEmail);
            
            response.put("success", true);
            response.put("message", "Connexion SMTP réussie et email envoyé avec succès !");
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationFailedException e) {
            logger.error("❌ ÉCHEC D'AUTHENTIFICATION SMTP - Host: {}, Port: {}, User: {}", 
                host, port, username, e);
            
            // Détection automatique du type de problème
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String detectedIssue = detectPasswordIssue(errorMsg, password);
            
            String errorMessage = String.format(
                "%sÉchec d'authentification SMTP. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                detectedIssue.isEmpty() ? "" : detectedIssue + " - ", 
                host, port, username, e.getMessage()
            );
            
            String suggestions = generateSuggestions(errorMsg, password, true);
            
            response.put("success", false);
            response.put("error", "AuthenticationFailedException");
            response.put("message", errorMessage);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            response.put("isPasswordIssue", !detectedIssue.isEmpty());
            response.put("detectedIssue", detectedIssue);
            response.put("suggestions", suggestions);
            response.put("passwordLength", password != null ? password.length() : 0);
            response.put("passwordPreview", password != null ? password : "***");
            
            return ResponseEntity.status(401).body(response);
            
        } catch (MessagingException e) {
            logger.error("❌ Erreur lors du test SMTP: {}", e.getMessage(), e);
            
            // Détecter si c'est un problème de mot de passe (timeout peut être causé par mauvais mot de passe)
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String causeMsg = "";
            boolean isPasswordIssue = false;
            
            if (e.getCause() != null) {
                causeMsg = e.getCause().getMessage() != null ? e.getCause().getMessage().toLowerCase() : "";
                // Un timeout lors de la connexion peut indiquer un problème d'authentification
                if (causeMsg.contains("timeout") || causeMsg.contains("read timed out") || 
                    causeMsg.contains("connection timed out") || causeMsg.contains("socket")) {
                    isPasswordIssue = true; // Très probablement un problème de mot de passe
                }
            }
            
            // Vérifier aussi dans le message d'erreur principal
            if (errorMsg.contains("authentication") || errorMsg.contains("auth") || 
                errorMsg.contains("password") || errorMsg.contains("credentials") ||
                errorMsg.contains("535") || errorMsg.contains("login")) {
                isPasswordIssue = true;
            }
            
            String passwordIssue = isPasswordIssue 
                ? " ⚠️ PROBLÈME DE MOT DE PASSE PROBABLE (timeout/authentification) - " 
                : "";
            
            String errorMessage = String.format(
                "%sErreur de messagerie SMTP. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                passwordIssue, host, port, username, e.getMessage()
            );
            
            String suggestions = isPasswordIssue
                ? "⚠️ PROBLÈME DE MOT DE PASSE DÉTECTÉ (timeout peut indiquer authentification échouée): 1) Vérifiez que le mot de passe dans application-prod.properties est EXACTEMENT correct, 2) Vérifiez qu'il n'y a pas d'espaces avant/après, 3) Les caractères spéciaux ($, @, *, ?, etc.) doivent être correctement configurés, 4) Un timeout 'Read timed out' lors de la connexion SMTP indique souvent un mauvais mot de passe, 5) Essayez de vous connecter manuellement avec ce mot de passe"
                : "Vérifiez: 1) La connexion réseau au serveur SMTP, 2) Le serveur est accessible, 3) Le firewall n'bloque pas la connexion, 4) Les timeouts sont suffisants";
            
            response.put("success", false);
            response.put("error", "MessagingException");
            response.put("message", errorMessage);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            response.put("isPasswordIssue", isPasswordIssue);
            response.put("suggestions", suggestions);
            response.put("passwordLength", password != null ? password.length() : 0);
            response.put("passwordPreview", password != null ? password : "***");
            
            // Ajouter la cause si disponible
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                response.put("causeType", e.getCause().getClass().getSimpleName());
            }
            
            return ResponseEntity.status(500).body(response);
            
        } catch (Exception e) {
            logger.error("❌ Erreur inattendue lors du test SMTP: {}", e.getMessage(), e);
            
            String errorMessage = String.format(
                "Erreur inattendue lors du test SMTP. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                host, port, username, e.getMessage()
            );
            
            response.put("success", false);
            response.put("error", "UnexpectedException");
            response.put("message", errorMessage);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            
            // Ajouter la cause si disponible
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/mail/connection/facture")
    public ResponseEntity<Map<String, Object>> testFactureMailConnection(
            @RequestParam(required = false) String testEmail) {
        
        Map<String, Object> response = new HashMap<>();
        
        String recipientEmail = testEmail != null && !testEmail.isEmpty() 
            ? testEmail 
            : "carterhedy57@gmail.com";
        
        logger.info("🧪 Test de connexion SMTP FACTURE - Host: {}, Port: {}, User: {}, Recipient: {}", 
            host, port, factureUsername, recipientEmail);
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
            props.put("mail.smtp.starttls.enable", "false");
            
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(factureUsername, facturePassword);
                }
            });
            
            Transport transport = session.getTransport("smtp");
            transport.connect(host, port, factureUsername, facturePassword);
            transport.close();
            
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(factureUsername, "Tchakeda"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Test SMTP Facture - Tchakeda");
            message.setText("Ceci est un email de test pour le compte facture.");
            
            Transport.send(message);
            
            response.put("success", true);
            response.put("message", "Connexion SMTP FACTURE réussie et email envoyé !");
            response.put("username", factureUsername);
            response.put("recipient", recipientEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationFailedException e) {
            logger.error("❌ ÉCHEC D'AUTHENTIFICATION SMTP FACTURE - Host: {}, Port: {}, User: {}", 
                host, port, factureUsername, e);
            
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String detectedIssue = detectPasswordIssue(errorMsg, facturePassword);
            
            String errorMessage = String.format(
                "%sÉchec d'authentification SMTP FACTURE. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                detectedIssue.isEmpty() ? "" : detectedIssue + " - ", 
                host, port, factureUsername, e.getMessage()
            );
            
            String suggestions = generateSuggestions(errorMsg, facturePassword, true);
            
            response.put("success", false);
            response.put("error", "AuthenticationFailedException");
            response.put("message", errorMessage);
            response.put("host", host);
            response.put("port", port);
            response.put("username", factureUsername);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            response.put("isPasswordIssue", !detectedIssue.isEmpty());
            response.put("detectedIssue", detectedIssue);
            response.put("suggestions", suggestions);
            response.put("passwordLength", facturePassword != null ? facturePassword.length() : 0);
            response.put("passwordPreview", facturePassword != null ? facturePassword : "***");
            
            return ResponseEntity.status(401).body(response);
            
        } catch (MessagingException e) {
            logger.error("❌ Erreur test SMTP FACTURE: {}", e.getMessage(), e);
            
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String causeMsg = "";
            
            if (e.getCause() != null) {
                causeMsg = e.getCause().getMessage() != null ? e.getCause().getMessage().toLowerCase() : "";
            }
            
            String combinedError = errorMsg + " " + causeMsg;
            String detectedIssue = detectPasswordIssue(combinedError, facturePassword);
            
            String errorMessage = String.format(
                "%sErreur de messagerie SMTP FACTURE. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                detectedIssue.isEmpty() ? "" : detectedIssue + " - ", 
                host, port, factureUsername, e.getMessage()
            );
            
            String suggestions = generateSuggestions(combinedError, facturePassword, false);
            
            response.put("success", false);
            response.put("error", "MessagingException");
            response.put("message", errorMessage);
            response.put("host", host);
            response.put("port", port);
            response.put("username", factureUsername);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            response.put("isPasswordIssue", !detectedIssue.isEmpty());
            response.put("detectedIssue", detectedIssue);
            response.put("suggestions", suggestions);
            response.put("passwordLength", facturePassword != null ? facturePassword.length() : 0);
            response.put("passwordPreview", facturePassword != null ? facturePassword : "***");
            
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                response.put("causeType", e.getCause().getClass().getSimpleName());
            }
            
            return ResponseEntity.status(500).body(response);
            
        } catch (Exception e) {
            logger.error("❌ Erreur inattendue test SMTP FACTURE: {}", e.getMessage(), e);
            
            String errorMessage = String.format(
                "Erreur inattendue lors du test SMTP FACTURE. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                host, port, factureUsername, e.getMessage()
            );
            
            response.put("success", false);
            response.put("error", "UnexpectedException");
            response.put("message", errorMessage);
            response.put("host", host);
            response.put("port", port);
            response.put("username", factureUsername);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/mail/config")
    public ResponseEntity<Map<String, Object>> getMailConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("host", host);
        config.put("port", port);
        config.put("username", username);
        config.put("factureUsername", factureUsername);
        // Ne pas exposer les mots de passe
        config.put("passwordLength", password != null ? password.length() : 0);
        config.put("facturePasswordLength", facturePassword != null ? facturePassword.length() : 0);
        return ResponseEntity.ok(config);
    }

    /**
     * Détecte automatiquement le type de problème de mot de passe
     */
    private String detectPasswordIssue(String errorMsg, String password) {
        if (errorMsg == null || errorMsg.isEmpty()) {
            return "";
        }
        
        String lowerError = errorMsg.toLowerCase();
        
        // Détection spécifique des codes d'erreur SMTP liés au mot de passe
        if (lowerError.contains("535") || lowerError.contains("535-5.7.8") || 
            lowerError.contains("535-5.7.1") || lowerError.contains("invalid login")) {
            return "⚠️ MOT DE PASSE INCORRECT (Code SMTP 535)";
        }
        
        // Détection des messages d'authentification explicites
        if (lowerError.contains("authentication failed") || 
            lowerError.contains("invalid credentials") ||
            lowerError.contains("login failed") ||
            lowerError.contains("wrong password") ||
            lowerError.contains("incorrect password")) {
            return "⚠️ MOT DE PASSE INCORRECT (Authentification échouée)";
        }
        
        // Détection des timeouts qui peuvent indiquer un problème de mot de passe
        if (lowerError.contains("read timed out") || 
            lowerError.contains("connection timed out") ||
            lowerError.contains("socket timeout")) {
            // Vérifier si le mot de passe semble valide
            if (password == null || password.isEmpty() || password.length() < 4) {
                return "⚠️ MOT DE PASSE MANQUANT OU TROP COURT";
            }
            return "⚠️ TIMEOUT (probablement mot de passe incorrect)";
        }
        
        // Détection générique
        if (lowerError.contains("password") || 
            lowerError.contains("mot de passe") ||
            lowerError.contains("auth") && lowerError.contains("fail")) {
            return "⚠️ PROBLÈME D'AUTHENTIFICATION (possiblement mot de passe)";
        }
        
        return "";
    }

    /**
     * Génère des suggestions basées sur le type d'erreur détecté
     */
    private String generateSuggestions(String errorMsg, String password, boolean isAuthException) {
        if (errorMsg == null) errorMsg = "";
        String lowerError = errorMsg.toLowerCase();
        
        // Vérifier si le mot de passe est vide ou trop court
        if (password == null || password.isEmpty()) {
            return "❌ MOT DE PASSE MANQUANT: Le mot de passe n'est pas configuré dans application-prod.properties";
        }
        
        if (password.length() < 4) {
            return "❌ MOT DE PASSE TROP COURT: Le mot de passe doit contenir au moins 4 caractères";
        }
        
        // Suggestions pour code SMTP 535
        if (lowerError.contains("535")) {
            return "❌ CODE SMTP 535 (Mauvais mot de passe): 1) Vérifiez que le mot de passe est EXACTEMENT correct, 2) Pas d'espaces avant/après, 3) Caractères spéciaux correctement configurés";
        }
        
        // Suggestions pour timeout
        if (lowerError.contains("timeout") || lowerError.contains("read timed out")) {
            return "⏱️ TIMEOUT DÉTECTÉ: Un timeout lors de la connexion SMTP indique souvent un mauvais mot de passe. Le serveur ne répond pas car l'authentification échoue. Vérifiez: 1) Le mot de passe est correct, 2) Pas d'espaces, 3) Caractères spéciaux bien configurés";
        }
        
        // Suggestions pour authentification échouée
        if (lowerError.contains("authentication failed") || lowerError.contains("invalid credentials")) {
            return "🔐 AUTHENTIFICATION ÉCHOUÉE: 1) Mot de passe incorrect, 2) Vérifiez les caractères spéciaux ($, @, *, ?, etc.), 3) Essayez de vous connecter manuellement";
        }
        
        // Suggestions génériques pour problèmes de mot de passe
        if (lowerError.contains("password") || lowerError.contains("auth")) {
            return "🔑 PROBLÈME D'AUTHENTIFICATION: 1) Vérifiez le mot de passe dans application-prod.properties, 2) Vérifiez les caractères spéciaux, 3) Pas d'espaces";
        }
        
        // Suggestions génériques pour autres erreurs
        if (isAuthException) {
            return "Vérifiez: 1) Le mot de passe est correct, 2) Le compte email est actif, 3) L'authentification SMTP est activée";
        }
        
        return "Vérifiez: 1) La connexion réseau, 2) Le serveur est accessible, 3) Le firewall, 4) Les timeouts";
    }
}

