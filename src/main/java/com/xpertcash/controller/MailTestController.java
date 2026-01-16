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

    // Configuration pour le compte contact (inscriptions et autres)
    @Value("${spring.mail.contact.host}")
    private String contactHost;

    @Value("${spring.mail.contact.port}")
    private int contactPort;

    @Value("${spring.mail.contact.username}")
    private String contactUsername;

    @Value("${spring.mail.contact.password}")
    private String contactPassword;

    // Configuration pour le compte facture
    @Value("${spring.mail.facture.host}")
    private String factureHost;

    @Value("${spring.mail.facture.port}")
    private int facturePort;

    @Value("${spring.mail.facture.username}")
    private String factureUsername;

    @Value("${spring.mail.facture.password}")
    private String facturePassword;

    @PostMapping("/mail/connection")
    public ResponseEntity<Map<String, Object>> testMailConnection(
            @RequestParam(required = false) String testEmail) {
        return testMailConnectionInternal(contactHost, contactPort, contactUsername, contactPassword, testEmail, "contact");
    }

    @PostMapping("/mail/facture/connection")
    public ResponseEntity<Map<String, Object>> testFactureMailConnection(
            @RequestParam(required = false) String testEmail) {
        return testMailConnectionInternal(factureHost, facturePort, factureUsername, facturePassword, testEmail, "facture");
    }

    private ResponseEntity<Map<String, Object>> testMailConnectionInternal(
            String host, int port, String username, String password, String testEmail, String accountType) {
        
        Map<String, Object> response = new HashMap<>();
        
        String recipientEmail = testEmail != null && !testEmail.isEmpty() 
            ? testEmail 
            : "carterhedy57@gmail.com";
        
        logger.info(" Test de connexion SMTP [{}] - Host: {}, Port: {}, User: {}, Recipient: {}", 
            accountType, host, port, username, recipientEmail);
        
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
            
            // Désactivation de STARTTLS pour le port 465
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
            logger.info(" Connexion SMTP réussie !");
            transport.close();
            
            // Si la connexion réussit, tester l'envoi d'un email
            logger.info("Tentative d'envoi d'un email de test...");
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "Tchakeda"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Test SMTP - Tchakeda [" + accountType + "]");
            message.setText("Ceci est un email de test pour vérifier la configuration SMTP.\n\n" +
                          "Compte utilisé: " + accountType + "\n" +
                          "Si vous recevez cet email, la configuration est correcte !");
            
            Transport.send(message);
            logger.info(" Email de test envoyé avec succès à {}", recipientEmail);
            
            response.put("success", true);
            response.put("message", "Connexion SMTP réussie et email envoyé avec succès !");
            response.put("accountType", accountType);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationFailedException e) {
            logger.error(" ÉCHEC D'AUTHENTIFICATION SMTP [{}] - Host: {}, Port: {}, User: {}", 
                accountType, host, port, username, e);
            
            // Détection automatique du type de problème
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String detectedIssue = detectPasswordIssue(errorMsg, password);
            
            String errorMessage = String.format(
                "%sÉchec d'authentification SMTP. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                detectedIssue.isEmpty() ? "" : detectedIssue + " - ", 
                host, port, username, e.getMessage()
            );
            
            
            response.put("success", false);
            response.put("error", "AuthenticationFailedException");
            response.put("message", errorMessage);
            response.put("accountType", accountType);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            response.put("isPasswordIssue", !detectedIssue.isEmpty());
            response.put("detectedIssue", detectedIssue);
            response.put("passwordLength", password != null ? password.length() : 0);
            response.put("passwordPreview", password);
            
            return ResponseEntity.status(401).body(response);
            
        } catch (MessagingException e) {
            logger.error(" Erreur lors du test SMTP [{}]: {}", accountType, e.getMessage(), e);
            
            // Détecter si c'est un problème de mot de passe (timeout peut être causé par mauvais mot de passe)
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String causeMsg = "";
            boolean isPasswordIssue = false;
            
            if (e.getCause() != null) {
                causeMsg = e.getCause().getMessage() != null ? e.getCause().getMessage().toLowerCase() : "";
                // Un timeout lors de la connexion peut indiquer un problème d'authentification
                if (causeMsg.contains("timeout") || causeMsg.contains("read timed out") || 
                    causeMsg.contains("connection timed out") || causeMsg.contains("socket")) {
                    isPasswordIssue = true; 
                }
            }
            
            // Vérifier aussi dans le message d'erreur principal
            if (errorMsg.contains("authentication") || errorMsg.contains("auth") || 
                errorMsg.contains("password") || errorMsg.contains("credentials") ||
                errorMsg.contains("535") || errorMsg.contains("login")) {
                isPasswordIssue = true;
            }
            
            String passwordIssue = isPasswordIssue 
                ? "  PROBLÈME DE MOT DE PASSE PROBABLE (timeout/authentification) - " 
                : "";
            
            String errorMessage = String.format(
                "%sErreur de messagerie SMTP. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                passwordIssue, host, port, username, e.getMessage()
            );
            
            String suggestions = isPasswordIssue
                ? " PROBLÈME DE MOT DE PASSE DÉTECTÉ (timeout peut indiquer authentification échouée): 1) "
                : "))";
            
            response.put("success", false);
            response.put("error", "MessagingException");
            response.put("message", errorMessage);
            response.put("accountType", accountType);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            response.put("isPasswordIssue", isPasswordIssue);
            response.put("suggestions", suggestions);
            response.put("passwordLength", password != null ? password.length() : 0);
            response.put("passwordPreview", password);
            
            //   cause si disponible
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                response.put("causeType", e.getCause().getClass().getSimpleName());
            }
            
            return ResponseEntity.status(500).body(response);
            
        } catch (Exception e) {
            logger.error(" Erreur inattendue lors du test SMTP [{}]: {}", accountType, e.getMessage(), e);
            
            String errorMessage = String.format(
                "Erreur inattendue lors du test SMTP. Détails: Host=%s, Port=%d, Username=%s, Erreur=%s",
                host, port, username, e.getMessage()
            );
            
            response.put("success", false);
            response.put("error", "UnexpectedException");
            response.put("message", errorMessage);
            response.put("accountType", accountType);
            response.put("host", host);
            response.put("port", port);
            response.put("username", username);
            response.put("recipient", recipientEmail);
            response.put("errorType", e.getClass().getName());
            response.put("errorDetails", e.getMessage());
            
            //  si disponible
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/mail/config")
    public ResponseEntity<Map<String, Object>> getMailConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Configuration contact
        Map<String, Object> contactConfig = new HashMap<>();
        contactConfig.put("host", contactHost);
        contactConfig.put("port", contactPort);
        contactConfig.put("username", contactUsername);
        contactConfig.put("passwordLength", contactPassword != null ? contactPassword.length() : 0);
        contactConfig.put("passwordPreview", contactPassword);
        
        // Configuration facture
        Map<String, Object> factureConfig = new HashMap<>();
        factureConfig.put("host", factureHost);
        factureConfig.put("port", facturePort);
        factureConfig.put("username", factureUsername);
        factureConfig.put("passwordLength", facturePassword != null ? facturePassword.length() : 0);
        factureConfig.put("passwordPreview", facturePassword);
        
        config.put("contact", contactConfig);
        config.put("facture", factureConfig);
        
        return ResponseEntity.ok(config);
    }

   
    private String detectPasswordIssue(String errorMsg, String password) {
        if (errorMsg == null || errorMsg.isEmpty()) {
            return "";
        }
        
        String lowerError = errorMsg.toLowerCase();
        
        // Détection spécifique des codes d'erreur SMTP liés au mot de passe
        if (lowerError.contains("535") || lowerError.contains("535-5.7.8") || 
            lowerError.contains("535-5.7.1") || lowerError.contains("invalid login")) {
            return " MOT DE PASSE INCORRECT (Code SMTP 535)";
        }
        
        // Détection des messages d'authentification explicites
        if (lowerError.contains("authentication failed") || 
            lowerError.contains("invalid credentials") ||
            lowerError.contains("login failed") ||
            lowerError.contains("wrong password") ||
            lowerError.contains("incorrect password")) {
            return " MOT DE PASSE INCORRECT (Authentification échouée)";
        }
        
        // Détection des timeouts qui peuvent indiquer un problème de mot de passe
        if (lowerError.contains("read timed out") || 
            lowerError.contains("connection timed out") ||
            lowerError.contains("socket timeout")) {
            // Vérifier si le mot de passe semble valide
            if (password == null || password.isEmpty() || password.length() < 4) {
                return " MOT DE PASSE MANQUANT OU TROP COURT";
            }
            return " TIMEOUT (probablement mot de passe incorrect)";
        }
        
        // Détection générique
        if (lowerError.contains("password") || 
            lowerError.contains("mot de passe") ||
            lowerError.contains("auth") && lowerError.contains("fail")) {
            return " PROBLÈME D'AUTHENTIFICATION (possiblement mot de passe)";
        }
        
        return "";
    }

   

}

