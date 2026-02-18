package com.xpertcash.service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.xpertcash.DTOs.VENTE.ReceiptEmailRequest;
import com.xpertcash.DTOs.VENTE.VenteLigneResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;
 
    @Autowired
    @Qualifier("factureMailSender")
    private JavaMailSender factureMailSender;
 
    @Value("${spring.mail.contact.username}")
    private String from; 

    @Value("${spring.mail.facture.username}")
    private String factureFrom;

    @Value("${spring.mail.contact.host}")
    private String mailHost;

    @Value("${spring.mail.contact.port}")
    private int mailPort;

    @Value("${app.frontend.url:https://fere.tchakeda.com}")
    private String frontendUrl;

    public void sendActivationLinkEmail(String to, String code, String personalCode) throws MessagingException {
        String baseUrl = "https://xpertcash.tchakeda.com/api/v1";
        String activationUrl = baseUrl + "/api/auth/activate?email=" + to + "&code=" + code;

        String subject = "Activation de votre compte";
        String htmlContent = generateActivationEmail(personalCode, activationUrl);

        sendEmail(to, subject, htmlContent);
    }


    public void sendEmployeEmail(String to, String fullName, String companyName, String role, String email, String password, String personalCode) throws MessagingException {
        String subject = "Création de votre compte Tchakeda";
        String htmlContent = generateInfoEmail(fullName, companyName, role, email, password, personalCode);
        sendEmail(to, subject, htmlContent);
    }
    
    
     // Méthode d'envoi d'email pour relancer une facture
     public void sendRelanceeEmail(String to, String fullName, String factureNumero, String clientName, Date relanceDate, boolean estEntreprise) throws MessagingException {
        System.out.println(" Envoi d'un email à : " + to);
        String subject = "Relance de la facture " + factureNumero;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formattedDate = sdf.format(relanceDate);
        String message = generateFactureRelanceMessage(factureNumero, clientName, formattedDate, estEntreprise);
        sendFactureEmail(to, subject, message);
    }
    


    public void sendUnlockLinkEmail(String to, String code) throws MessagingException {
        String baseUrl = "https://xpertcash.tchakeda.com/api/v1";
        String unlockUrl = baseUrl + "/api/auth/unlock?email=" + to + "&code=" + code;

        String subject = "Déverrouillage de votre compte";
        String htmlContent = generateUnlockEmail(unlockUrl);

        sendEmail(to, subject, htmlContent);
    }

    public void sendEmailVerificationCode(String toEmail, String verificationCode) throws MessagingException {
        String subject = "Vérification de changement d'email";
        String htmlContent = generateVerificationEmail(verificationCode);

        sendEmail(toEmail, subject, htmlContent);
    }
    
    //ici
    public void sendEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
    logger.info(" Tentative d'envoi d'email - Destinataire: {}, Sujet: {}, Expéditeur: {}, Host: {}, Port: {}", 
        toEmail, subject, from, mailHost, mailPort);
    
    try {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(from, "Tchakeda");
            logger.debug("Expéditeur configuré: {}", from);
        } catch (UnsupportedEncodingException e) {
            logger.error(" Erreur lors de la configuration de l'expéditeur: {}", e.getMessage(), e);
            throw new MessagingException("Erreur lors de la configuration de l'expéditeur", e);
        }
    helper.setTo(toEmail);
    helper.setSubject(subject);
    helper.setText(htmlContent, true);

    try {
        InputStream logoStream = getClass().getClassLoader().getResourceAsStream("assets/tchakeda.png");
        if (logoStream == null) {
                logger.warn(" Logo image not found in resources");
            throw new MessagingException("Logo image not found in resources.");
        }

        ByteArrayDataSource logoDataSource = new ByteArrayDataSource(logoStream, "image/png");
        helper.addInline("logo", logoDataSource);
            logger.debug("Logo ajouté au message");
    } catch (IOException e) {
            logger.error(" Erreur lors du chargement du logo: {}", e.getMessage(), e);
        throw new MessagingException("Error loading logo image", e);
    }

        logger.info("Envoi du message email en cours...");
    mailSender.send(message);
        logger.info(" Email envoyé avec succès à {}", toEmail);
    } catch (jakarta.mail.AuthenticationFailedException e) {
        logger.error(" ÉCHEC D'AUTHENTIFICATION EMAIL - Host: {}, Port: {}, User: {}, Erreur: {}", 
            mailHost, mailPort, from, e.getMessage(), e);
        throw new MessagingException("Échec d'authentification email: " + e.getMessage(), e);
    } catch (MessagingException e) {
        logger.error(" Erreur lors de l'envoi de l'email à {} - Erreur: {}", toEmail, e.getMessage(), e);
        throw e;
    } catch (Exception e) {
        logger.error(" Erreur inattendue lors de l'envoi de l'email à {} - Erreur: {}", toEmail, e.getMessage(), e);
        throw new MessagingException("Erreur inattendue lors de l'envoi de l'email", e);
    }
}

    public void sendPasswordResetEmail(String to, String otp) throws MessagingException {
        String subject = "Réinitialisation de votre mot de passe";
        String htmlContent = generatePasswordResetEmail(otp);
    
        sendEmail(to, subject, htmlContent);
    }

    // Méthode pour envoyer des emails de facture avec le compte facture@tchakeda.com
    public void sendFactureEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        logger.info(" Tentative d'envoi d'email FACTURE - Destinataire: {}, Sujet: {}, Expéditeur: {}", 
            toEmail, subject, factureFrom);
    
        try {
            MimeMessage message = factureMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            try {
                helper.setFrom(factureFrom, "Tchakeda");
                logger.debug("Expéditeur facture configuré: {}", factureFrom);
            } catch (UnsupportedEncodingException e) {
                logger.error(" Erreur lors de la configuration de l'expéditeur facture: {}", e.getMessage(), e);
                throw new MessagingException("Erreur lors de la configuration de l'expéditeur facture", e);
            }
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            try {
                InputStream logoStream = getClass().getClassLoader().getResourceAsStream("assets/tchakeda.png");
                if (logoStream == null) {
                    logger.warn(" Logo image not found in resources");
                    throw new MessagingException("Logo image not found in resources.");
                }

                ByteArrayDataSource logoDataSource = new ByteArrayDataSource(logoStream, "image/png");
                helper.addInline("logo", logoDataSource);
                logger.debug("Logo ajouté au message facture");
            } catch (IOException e) {
                logger.error(" Erreur lors du chargement du logo: {}", e.getMessage(), e);
                throw new MessagingException("Error loading logo image", e);
            }

            logger.info("Envoi du message email facture en cours...");
            factureMailSender.send(message);
            logger.info(" Email facture envoyé avec succès à {}", toEmail);
        } catch (jakarta.mail.AuthenticationFailedException e) {
            logger.error(" ÉCHEC D'AUTHENTIFICATION EMAIL FACTURE - User: {}, Erreur: {}", 
                factureFrom, e.getMessage(), e);
            throw new MessagingException("Échec d'authentification email facture: " + e.getMessage(), e);
        } catch (MessagingException e) {
            logger.error(" Erreur lors de l'envoi de l'email facture à {} - Erreur: {}", toEmail, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error(" Erreur inattendue lors de l'envoi de l'email facture à {} - Erreur: {}", toEmail, e.getMessage(), e);
            throw new MessagingException("Erreur inattendue lors de l'envoi de l'email facture", e);
        }
    }

    // Méthode pour envoyer des emails de facture avec pièces jointes
    public void sendFactureEmailWithAttachments(
        String toEmail,
        String ccEmail,
        String subject,
        String htmlContent,
        List<MultipartFile> attachments
    ) throws MessagingException, IOException {
        logger.info(" Tentative d'envoi d'email FACTURE avec pièces jointes - Destinataire: {}, Sujet: {}, Expéditeur: {}", 
            toEmail, subject, factureFrom);
        
        MimeMessage message = factureMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(factureFrom, "Tchakeda");
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Erreur lors de la configuration de l'expéditeur facture", e);
        }
        helper.setTo(toEmail.split(","));
        if (ccEmail != null && !ccEmail.isBlank()) {
            helper.setCc(ccEmail.split(","));
        }
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        InputStream logoStream = getClass().getClassLoader().getResourceAsStream("assets/tchakeda.png");
        if (logoStream != null) {
            ByteArrayDataSource logoDataSource = new ByteArrayDataSource(logoStream, "image/png");
            helper.addInline("logo", logoDataSource);
        } else {
            throw new MessagingException("Logo introuvable dans les resources.");
        }

        for (MultipartFile file : attachments) {
            if (!file.isEmpty()) {
                String contentType = file.getContentType();
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                helper.addAttachment(
                    Objects.requireNonNull(file.getOriginalFilename()),
                    new ByteArrayResource(file.getBytes()),
                    contentType
                );
            }
        }

        factureMailSender.send(message);
        logger.info(" Email facture avec pièces jointes envoyé avec succès à {}", toEmail);
    }

    
    private String generateActivationEmail(String personalCode ,String activationUrl) {
        return """
            <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                        <div style="max-width: 600px; margin: auto; background: white; padding: 17px; border-radius: 10px; text-align: center;">
                            <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: 10px;">
                            <h2 style="color: #333; margin-top: -9px; font-size: 19px;">Activation de votre compte</h2> 
                            <p>Bonjour,</p>
                            <p>Lors de votre inscription, vous bénéficiez de <strong>24h d'utilisation gratuite</strong> du système.</p>
                            <p>Votre code PIN est :<strong>%s</strong></p>
                            <p>Pour continuer à utiliser votre compte, veuillez l'activer en cliquant ci-dessous :</p>
                            <a href="%s" style="display: inline-block; padding: 12px 20px; background-color: rgb(19, 137, 247); color: white; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                Activer mon compte
                            </a>
                            <p style="font-size: 12px; color: #555; margin-top: 30px;">Si vous n'avez pas effectué cette demande, veuillez ignorer cet e-mail.</p>
                            <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
                            <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                        </div>
                    </body>
            </html>

        """.formatted(personalCode ,activationUrl);
    }


    private String generateInfoEmail(String fullName, String companyName, String role, String email, String password, String personalCode) {
        return """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 10px; text-align: center;">
                 <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: 10px;">
                <h2 style="color: #333; margin-top: -19px;font-size: 19px;">Bienvenue chez %s</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Vous venez d'être ajouté à l'entreprise <strong>%s</strong> en tant que <strong>%s</strong>.</p>
                <p>Voici vos identifiants de connexion :</p>
                <div style="padding: 10px; border-radius: 5px; display: inline-block; text-align: left;">
                    <p><strong>Email :</strong> %s</p>
                    <p><strong>Mot de passe :</strong> %s</p>
                    <p><strong>Code PIN :</strong> %s</p>
                </div>
                <p style="margin-top: 8px;">Nous vous recommandons de changer votre mot de passe dès votre première connexion.</p>
                <a href="https://fere.tchakeda.com/connexion" style="display: inline-block; padding: 12px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 10px;">
                    Se connecter
                </a>
                <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
                <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
            </div>
        </body>
        </html>
        """.formatted(companyName, fullName, companyName, role, email, password, personalCode);
    }
    
    
        private String generateUnlockEmail(String unlockUrl) {
        return """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 10px; text-align: center;">
                <h2 style="color: #333;">Déverrouillage de votre compte</h2>
                <p>Votre compte a été verrouillé en raison d'une inactivité de 30 minutes.</p>
                <p>Pour le déverrouiller, cliquez sur le bouton ci-dessous :</p>
                <a href="%s" style="display: inline-block; padding: 12px 20px; background-color: #dc3545; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;">
                    Déverrouiller mon compte
                </a>
            </div>
        </body>
        </html>
        """.formatted(unlockUrl);
    }

    private String generateVerificationEmail(String verificationCode) {
        return """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 10px; text-align: center;">
                <h2 style="color: #333;">Vérification de changement d'email</h2>
                <p>Votre code de vérification est :</p>
                <h3 style="background: #eee; padding: 10px; display: inline-block; border-radius: 5px;">%s</h3>
                <p>Ce code est valable pour une durée limitée.</p>
            </div>
        </body>
        </html>
        """.formatted(verificationCode);
    }


    private String generatePasswordResetEmail(String otp) {
        return """
            <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; padding: 17px; border-radius: 10px; text-align: center;">
                        <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: -26px;">
                        <p><strong>Bonjour</strong>,</p>
                        <p>Votre code de vérification est :</p>
                        <h3 style="background: #f8d7da; padding: 10px; border-radius: 5px; display: inline-block;">
                            %s
                        </h3>
                        <p>Ce code est valable pendant <strong>10 minutes</strong>.</p>
                        <p style="font-size: 12px; color: #555; margin-top: 30px;">Si vous n'avez pas effectué cette demande, veuillez ignorer cet e-mail.</p>
                       <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
                        <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                    </div>
                </body>
            </html>
        """.formatted(otp);
    }


     // Envoi une notification par email pour relancer une facture
     private String generateFactureRelanceMessage(String factureNumero, String clientName, String relanceDate, boolean estEntreprise) {
        String destinataireLabel = estEntreprise ? "l'entreprise" : "le client";
    
        return """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 10px; text-align: center;">
                    <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: 10px;">
                    <h2 style="color: #333; margin-top: -19px; font-size: 19px;">Relance de la facture</h2>
                    <p>Bonjour,</p>
                    <p>La facture <strong style="font-style: italic;">%s</strong> pour %s <strong>%s</strong> doit être relancée.</p>
                    <p>La date de relance prévue était : <strong>%s</strong>.</p>
                    <p>Veuillez effectuer la relance.</p>
                    <p style="font-size: 12px; color: #555; margin-top: 30px;">Si vous n'avez pas effectué cette demande, veuillez ignorer cet e-mail.</p>
                    <p style="font-size: 10px; color: #777; margin-top: 30px;">L'équipe Tchakeda</p>
                    <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                </div>
            </body>
            </html>
        """.formatted(factureNumero, destinataireLabel, clientName, relanceDate);
    }

    // Méthode d'envoi d'email pour demande d'approbation de facture
    public void sendDemandeApprobationEmail(String to, String fullName, String factureNumero, String createurNom, String montantTotal, String objetFacture, Long factureId, HttpServletRequest request) throws MessagingException {
        System.out.println(" Envoi d'un email d'approbation à : " + to);
        String subject = "Demande d'approbation - Facture " + factureNumero;
        String htmlContent = generateDemandeApprobationMessage(fullName, factureNumero, createurNom, montantTotal, objetFacture, factureId, request);
        sendFactureEmail(to, subject, htmlContent);
    }

    // Génération du message HTML pour la demande d'approbation
    private String generateDemandeApprobationMessage(String fullName, String factureNumero, String createurNom, String montantTotal, String objetFacture, Long factureId, HttpServletRequest request) {
        String objetDisplay = (objetFacture != null && !objetFacture.isBlank()) ? objetFacture : "Aucun objet spécifié";
        // Encoder l'ID comme le frontend (t_ + base64)
        String encodedId = encodeFactureId(factureId);
        // Détecter l'URL frontend selon l'origine de la requête
        String detectedFrontendUrl = detectFrontendUrl(request);
        String factureUrl = detectedFrontendUrl + "/facture-proforma-details/" + encodedId;
        
        return """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 17px; border-radius: 10px; text-align: center;">
                    <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: 10px;">
                    <h2 style="color: #333; margin-top: -9px; font-size: 19px;">Demande d'approbation de facture</h2>
                    <p style="text-align: start;">Bonjour <strong>%s</strong>,</p>
                    <p style="text-align: left; line-height: 1.6;">
                        Une nouvelle facture nécessite votre approbation. <br> 
                        La facture <strong>%s</strong> créée par <strong>%s</strong> 
                        pour un montant total de <strong>%s FCFA</strong> nécessite votre validation. 
                        <br><br>
                        <strong>Objet de la facture :</strong> %s
                        <br><br>
                        Veuillez cliquer sur le bouton ci-dessous pour examiner les détails et approuver cette facture.
                    </p>
                    <a href="%s" style="display: inline-block; padding: 12px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 10px;">
                        Voir la facture
                    </a>
                    <p style="font-size: 12px; color: #555; margin-top: 30px;">Si vous n'avez pas effectué cette demande, veuillez ignorer cet e-mail.</p>
                    <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
                    <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                </div>
            </body>
            </html>
        """.formatted(fullName, factureNumero, createurNom, montantTotal, objetDisplay, factureUrl);
    }

    // Méthode d'envoi d'email pour notification d'approbation de facture
    public void sendFactureApprouveeEmail(String to, String fullName, String factureNumero, String approbateurNom, String montantTotal, String objetFacture) throws MessagingException {
        System.out.println(" Envoi d'un email d'approbation confirmée à : " + to);
        String subject = "Facture approuvée - " + factureNumero;
        String htmlContent = generateFactureApprouveeMessage(fullName, factureNumero, approbateurNom, montantTotal, objetFacture);
        sendFactureEmail(to, subject, htmlContent);
    }

    // Génération du message HTML pour la notification d'approbation
    private String generateFactureApprouveeMessage(String fullName, String factureNumero, String approbateurNom, String montantTotal, String objetFacture) {
        String objetDisplay = (objetFacture != null && !objetFacture.isBlank()) ? objetFacture : "Aucun objet spécifié";
        
        return """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 17px; border-radius: 10px; text-align: center;">
                    <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: 10px;">
                    <h2 style="color: #333; margin-top: -9px; font-size: 19px;">Facture approuvée</h2>
                    <p style="text-align: start;">Bonjour <strong>%s</strong>,</p>
                    <p style="text-align: left; line-height: 1.6;">
                        Votre facture <strong>%s</strong> a été approuvée par <strong>%s</strong>. 
                        La facture d'un montant total de <strong>%s FCFA</strong> a été validée avec succès.
                        <br><br>
                        <strong>Objet de la facture :</strong> %s
                        <br><br>
                        Vous pouvez maintenant procéder aux prochaines étapes de traitement de cette facture.
                    </p>
                    <p style="font-size: 12px; color: #555; margin-top: 30px;">Si vous n'avez pas effectué cette demande, veuillez ignorer cet e-mail.</p>
                    <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
                    <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                </div>
            </body>
            </html>
        """.formatted(fullName, factureNumero, approbateurNom, montantTotal, objetDisplay);
    }

    // Méthode d'envoi d'email pour notification de modification de facture
    public void sendFactureModifieeEmail(String to, String fullName, String factureNumero, String modificateurNom, String montantTotal, String objetFacture) throws MessagingException {
        System.out.println(" Envoi d'un email de modification de facture à : " + to);
        String subject = "Facture modifiée - " + factureNumero;
        String htmlContent = generateFactureModifieeMessage(fullName, factureNumero, modificateurNom, montantTotal, objetFacture);
        sendFactureEmail(to, subject, htmlContent);
    }

    // Génération du message HTML pour la notification de modification
    private String generateFactureModifieeMessage(String fullName, String factureNumero, String modificateurNom, String montantTotal, String objetFacture) {
        String objetDisplay = (objetFacture != null && !objetFacture.isBlank()) ? objetFacture : "Aucun objet spécifié";
        
        return """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 17px; border-radius: 10px; text-align: center;">
                    <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: 10px;">
                    <h2 style="color: #333; margin-top: -9px; font-size: 19px;">Facture modifiée</h2>
                    <p style="text-align: start;">Bonjour <strong>%s</strong>,</p>
                    <p style="text-align: left; line-height: 1.6;">
                        La facture <strong>%s</strong> a été modifiée par <strong>%s</strong>. 
                        Le montant total de la facture est maintenant de <strong>%s FCFA</strong>.
                        <br><br>
                        <strong>Objet de la facture :</strong> %s
                        <br><br>
                        Veuillez vous connecter à votre compte pour consulter les détails de la modification.
                    </p>
                    <p style="font-size: 12px; color: #555; margin-top: 30px;">Si vous n'avez pas effectué cette demande, veuillez ignorer cet e-mail.</p>
                    <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
                    <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                </div>
            </body>
            </html>
        """.formatted(fullName, factureNumero, modificateurNom, montantTotal, objetDisplay);
    }

    public void sendEmailWithAttachments(
        String toEmail,
        String ccEmail,
        String subject,
        String htmlContent,
        List<MultipartFile> attachments
) throws MessagingException, IOException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    try {
        helper.setFrom(from, "Tchakeda");
    } catch (UnsupportedEncodingException e) {
        throw new MessagingException("Erreur lors de la configuration de l'expéditeur", e);
    }
    helper.setTo(toEmail.split(","));
    if (ccEmail != null && !ccEmail.isBlank()) {
        helper.setCc(ccEmail.split(","));
    }
    helper.setSubject(subject);
    helper.setText(htmlContent, true);

    InputStream logoStream = getClass().getClassLoader().getResourceAsStream("assets/tchakeda.png");
    if (logoStream != null) {
        ByteArrayDataSource logoDataSource = new ByteArrayDataSource(logoStream, "image/png");
        helper.addInline("logo", logoDataSource);
    } else {
        throw new MessagingException("Logo introuvable dans les resources.");
    }

    for (MultipartFile file : attachments) {
        if (!file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            helper.addAttachment(
                Objects.requireNonNull(file.getOriginalFilename()),
                new ByteArrayResource(file.getBytes()),
                contentType
            );
        }
    }

    mailSender.send(message);
}


       // Méthode pour notifier apres achat de module
        public void sendConfirmationActivationEmail(String to,
                                            String nomModule,
                                            BigDecimal prixUnitaire,
                                            BigDecimal montantTotal,
                                            String devise,
                                            String nomCompletProprietaire,
                                            String pays,
                                            String adresse,
                                            String ville,
                                            String referenceTransaction,
                                            String nomEntreprise,
                                            int dureeMois) throws MessagingException {


    String subject = "Confirmation d'activation du module : " + nomModule;
    String htmlContent = generatePaymentConfirmationEmail(
                                nomModule,
                                prixUnitaire,
                                montantTotal, 
                                devise, 
                                nomCompletProprietaire,
                                pays,
                                adresse, 
                                ville,
                                referenceTransaction,
                                nomEntreprise,
                                dureeMois);

    sendEmail(to, subject, htmlContent);
}


    //mail pour notifier apres achat de module
   private String generatePaymentConfirmationEmail(String nomModule,
                                            BigDecimal prixUnitaire,
                                            BigDecimal montantTotal,
                                            String devise,
                                            String nomCompletProprietaire,
                                            String pays,
                                            String adresse,
                                            String ville,
                                            String referenceTransaction,
                                            String nomEntreprise,
                                            int dureeMois) {
    // Fonction pour formater les nombres avec séparateur de milliers et sans décimales inutiles
    Function<BigDecimal, String> formatMontant = (montant) -> {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.FRENCH);
        formatter.applyPattern("#,##0.##");
        return formatter.format(montant)
                     .replace(",", " ")
                     .replace(".00", "")
                     .replace(",00", ""); // Double sécurité pour les différents locales
    };

    String prixFormate = formatMontant.apply(prixUnitaire);
    String totalFormate = formatMontant.apply(montantTotal);

    return """
    <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 17px; border-radius: 10px; text-align: center;">
                
                <img src="cid:logo" alt="Logo" style="width: 100px; margin-bottom: -20px;">
                
                <h2 style="color: #028313; margin-top: -7px; font-size: 12px;">Confirmation de votre paiement</h2> 
                
                <p><span>Bonjour %s</span>,</p>
                <p style="font-size: 10px">
                    Nous vous confirmons la réception de votre paiement pour l'activation du module <span>%s</span> 
                    destiné à l'entreprise <span>%s</span>. Voici le récapitulatif de votre transaction :
                </p>

                <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                    <tr style="background-color: #f2f2f2;">
                        <th style="border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 10px">Description</th>
                        <th style="border: 1px solid #ddd; padding: 8px; text-align: center; font-size: 10px">Durée</th>
                        <th style="border: 1px solid #ddd; padding: 8px; text-align: right; font-size: 10px">Montant</th>
                    </tr>
                    <tr>
                        <td style="border: 1px solid #ddd; padding: 8px; font-size: 10px">Prix unitaire du module</td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: center; font-size: 9px">-</td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: right; font-size: 9px">%s %s</td>
                    </tr>
                    <tr>
                        <td style="border: 1px solid #ddd; padding: 8px; font-size: 10px">Abonnement</td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: center; font-size: 9px">%s mois</td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: right; font-size: 9px">-</td>
                    </tr>
                    <tr>
                        <td style="border: 1px solid #ddd; padding: 8px; font-size: 10px"><strong>Total</strong></td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: center; font-size: 9px">-</td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: right; font-size: 9px"><strong>%s %s</strong></td>
                    </tr>
                </table>

                <h4 style="text-align: left; font-size: 10px">Coordonnées du titulaire de la carte :</h4>
                <p style="text-align: left; font-size: 10px;">
                    <span>%s</span><br>
                    %s, %s, %s
                </p>

                <p style="text-align: left; font-size: 10px;">
                    <span>Référence de transaction :</span> %s
                </p>

                <p style="font-size: 9px; color: #555; margin-top: 30px;">
                    Si vous avez des questions, notre équipe reste à votre disposition.
                </p>
                
                <p style="font-size: 8px; color: #777;">
                    L'équipe Tchakeda
                </p>
                <p style="font-size: 6px; color: #666; margin-top: 5px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
            </div>
        </body>
    </html>
    """.formatted(
        nomCompletProprietaire,
        nomModule,
        nomEntreprise,
        prixFormate,
        devise,
        String.valueOf(dureeMois),
        totalFormate, 
        devise,
        nomCompletProprietaire,
        pays, adresse,
        ville,
        referenceTransaction
    );
}

    // Méthode pour envoyer une facture de vente par email
    public void sendReceiptEmail(ReceiptEmailRequest request) throws MessagingException {
        String subject = "Facture de vente - " + request.getNumeroFacture();
        String htmlContent = generateReceiptEmail(request);
        sendFactureEmail(request.getEmail(), subject, htmlContent);
    }

    // Méthode pour envoyer une facture de vente par email avec pièces jointes (PDF)
    public void sendReceiptEmailWithAttachments(ReceiptEmailRequest request, List<MultipartFile> attachments) 
            throws MessagingException, IOException {
        String subject = "Facture de vente - " + request.getNumeroFacture();
        String htmlContent = generateReceiptEmail(request);
        sendFactureEmailWithAttachments(request.getEmail(), null, subject, htmlContent, attachments);
    }

    // Génération du contenu HTML pour l'email de facture
    private String generateReceiptEmail(ReceiptEmailRequest request) {
        // Fonction pour formater les montants
        Function<BigDecimal, String> formatMontant = (montant) -> {
            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.FRENCH);
            formatter.applyPattern("#,##0.##");
            return formatter.format(montant)
                         .replace(",", " ")
                         .replace(".00", "")
                         .replace(",00", "");
        };

        String montantTotalFormate = formatMontant.apply(request.getMontantTotal());
        String montantPayeFormate = formatMontant.apply(request.getMontantPaye());
        String changeDueFormate = formatMontant.apply(request.getChangeDue());

        // Vérifier s'il y a des remises à afficher
        boolean hasRemiseGlobale = request.getRemiseGlobale() != null && request.getRemiseGlobale() > 0;
        boolean hasRemisesProduits = request.getRemisesProduits() != null && !request.getRemisesProduits().isEmpty();
        // Vérifier si des lignes ont réellement des remises (pour afficher la colonne)
        boolean hasLignesAvecRemise = false;
        if (request.getLignes() != null) {
            for (VenteLigneResponse ligne : request.getLignes()) {
                if (ligne.getRemise() != null && ligne.getRemise() > 0) {
                    hasLignesAvecRemise = true;
                    break;
                }
            }
        }

        // Calculer le montant total avant remise
        BigDecimal montantAvantRemise;
        if (hasRemiseGlobale) {
            // Si remise globale, calculer à rebours depuis le montant total
            BigDecimal montantTotal = request.getMontantTotal();
            BigDecimal tauxRemise = BigDecimal.valueOf(request.getRemiseGlobale()).divide(BigDecimal.valueOf(100));
            montantAvantRemise = montantTotal.divide(BigDecimal.ONE.subtract(tauxRemise), 2, java.math.RoundingMode.HALF_UP);
        } else {
            // Sinon, calculer depuis les prix unitaires et quantités
            montantAvantRemise = BigDecimal.ZERO;
            for (VenteLigneResponse ligne : request.getLignes()) {
                BigDecimal prixUnitaire = BigDecimal.valueOf(ligne.getPrixUnitaire());
                BigDecimal quantite = BigDecimal.valueOf(ligne.getQuantite());
                BigDecimal montantLigneAvantRemise = prixUnitaire.multiply(quantite);
                montantAvantRemise = montantAvantRemise.add(montantLigneAvantRemise);
            }
        }

        // Génération des lignes de produits
        StringBuilder lignesHtml = new StringBuilder();
        for (VenteLigneResponse ligne : request.getLignes()) {
            String prixUnitaireFormate = formatMontant.apply(BigDecimal.valueOf(ligne.getPrixUnitaire()));
            String montantLigneFormate = formatMontant.apply(BigDecimal.valueOf(ligne.getMontantLigne()));
            
            // Vérifier si cette ligne a une remise
            Double remiseLigne = ligne.getRemise() != null ? ligne.getRemise() : 0.0;
            boolean ligneHasRemise = remiseLigne > 0;
            
            if (hasLignesAvecRemise) {
                // Afficher avec colonne remise (vide si pas de remise pour cette ligne)
                String remiseCell = ligneHasRemise ? 
                    String.format("<td style=\"border: 1px solid #ddd; padding: 4px; text-align: right; font-size: 9px; color: #d32f2f;\">-%s</td>", 
                        String.format("%.1f%%", remiseLigne)) :
                    "<td style=\"border: 1px solid #ddd; padding: 4px; text-align: right; font-size: 9px;\">-</td>";
                
                lignesHtml.append(String.format("""
                    <tr>
                        <td style="border: 1px solid #ddd; padding: 4px; font-size: 9px">%s</td>
                        <td style="border: 1px solid #ddd; padding: 4px; text-align: center; font-size: 9px">%d</td>
                        <td style="border: 1px solid #ddd; padding: 4px; text-align: right; font-size: 9px">%s</td>
                        %s
                        <td style="border: 1px solid #ddd; padding: 4px; text-align: right; font-size: 9px">%s</td>
                    </tr>
                    """, 
                    ligne.getNomProduit(), 
                    ligne.getQuantite(), 
                    prixUnitaireFormate,
                    remiseCell,
                    montantLigneFormate
                ));
            } else {
                // Afficher sans colonne remise
                lignesHtml.append(String.format("""
                    <tr>
                        <td style="border: 1px solid #ddd; padding: 4px; font-size: 9px">%s</td>
                        <td style="border: 1px solid #ddd; padding: 4px; text-align: center; font-size: 9px">%d</td>
                        <td style="border: 1px solid #ddd; padding: 4px; text-align: right; font-size: 9px">%s</td>
                        <td style="border: 1px solid #ddd; padding: 4px; text-align: right; font-size: 9px">%s</td>
                    </tr>
                    """, 
                    ligne.getNomProduit(), 
                    ligne.getQuantite(), 
                    prixUnitaireFormate, 
                    montantLigneFormate
                ));
            }
        }

        // Construire l'en-tête de la colonne remise si nécessaire
        String colonneRemiseHeader = hasLignesAvecRemise ? 
            "<th style=\"border: 1px solid #ddd; padding: 4px; text-align: right;\">Remise</th>" : "";

        // Construire la section des remises dans les totaux
        StringBuilder remisesSection = new StringBuilder();
        if (hasRemiseGlobale) {
            BigDecimal montantRemiseGlobale = montantAvantRemise.multiply(
                BigDecimal.valueOf(request.getRemiseGlobale()).divide(BigDecimal.valueOf(100))
            );
            String remiseGlobaleFormate = formatMontant.apply(montantRemiseGlobale);
            String remiseGlobalePct = String.format("%.1f%%", request.getRemiseGlobale());
            remisesSection.append(String.format("""
                <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                    <span>Sous-total:&nbsp;</span>
                    <span> %s FCFA</span>
                </div>
                <div style="display: flex; justify-content: space-between; margin-bottom: 3px; color: #d32f2f;">
                    <span>Remise globale (%s):&nbsp;</span>
                    <span> -%s FCFA</span>
                </div>
                """,
                formatMontant.apply(montantAvantRemise),
                remiseGlobalePct,
                remiseGlobaleFormate
            ));
        } else if (hasRemisesProduits) {
            // Calculer le total des remises par produit
            BigDecimal totalRemisesProduits = BigDecimal.ZERO;
            for (VenteLigneResponse ligne : request.getLignes()) {
                if (ligne.getRemise() != null && ligne.getRemise() > 0) {
                    BigDecimal montantLigneAvantRemise = BigDecimal.valueOf(ligne.getPrixUnitaire())
                            .multiply(BigDecimal.valueOf(ligne.getQuantite()));
                    BigDecimal remiseLigne = montantLigneAvantRemise.multiply(
                        BigDecimal.valueOf(ligne.getRemise()).divide(BigDecimal.valueOf(100))
                    );
                    totalRemisesProduits = totalRemisesProduits.add(remiseLigne);
                }
            }
            if (totalRemisesProduits.compareTo(BigDecimal.ZERO) > 0) {
                remisesSection.append(String.format("""
                    <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                        <span>Sous-total:&nbsp;</span>
                        <span> %s FCFA</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 3px; color: #d32f2f;">
                        <span>Remises produits:&nbsp;</span>
                        <span> -%s FCFA</span>
                    </div>
                    """,
                    formatMontant.apply(montantAvantRemise),
                    formatMontant.apply(totalRemisesProduits)
                ));
            }
        }

        return String.format("""
            <html>
                <body style="font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px; margin: 0;">
                    <div style="max-width: 400px; margin: auto; background: white; padding: 15px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); border-radius: 8px;">
                        
                        <!-- Logo et Header -->
                        <div style="text-align: center; margin-bottom: 15px;">
                            <img src="cid:logo" alt="Logo" style="width: 60px; height: auto;">
                            <h2 style="color: #333; margin: 5px 0; font-size: 14px;">Facture</h2>
                        </div>
                        
                        <!-- Informations de la facture -->
                        <div style="margin-bottom: 15px; font-size: 10px; color: #333;">
                            <div style="margin-bottom: 2px;"><strong>Numéro fact :</strong> %s</div>
                            <div style="margin-bottom: 2px;">%s</div>
                            <div style="margin-bottom: 2px;"><strong>Vendeur :</strong> %s</div>
                            <div style="margin-bottom: 2px;"><strong>Boutique :</strong> %s</div>
                        </div>

                        <!-- Ligne de séparation -->
                        <div style="border-top: 1px dashed #ccc; margin: 10px 0;"></div>

                        <!-- Produits -->
                        <div style="margin-bottom: 10px;">
                            <table style="width: 100%%; border-collapse: collapse; font-size: 9px;">
                                <tr style="background-color: #f8f8f8;">
                                    <th style="border: 1px solid #ddd; padding: 4px; text-align: left;">Produit</th>
                                    <th style="border: 1px solid #ddd; padding: 4px; text-align: center;">Qté</th>
                                    <th style="border: 1px solid #ddd; padding: 4px; text-align: right;">Prix</th>
                                    %s
                                    <th style="border: 1px solid #ddd; padding: 4px; text-align: right;">Total</th>
                                </tr>
                                %s
                            </table>
                        </div>

                        <!-- Ligne de séparation -->
                        <div style="border-top: 1px dashed #ccc; margin: 10px 0;"></div>

                        <!-- Totaux -->
                        <div style="font-size: 10px; margin-bottom: 10px;">
                            %s
                            <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                                <span>Total:&nbsp;</span>
                                <span><strong> %s FCFA</strong></span>
                            </div>
                            <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                                <span>%s:&nbsp;</span>
                                <span> %s FCFA</span>
                            </div>
                            <div style="display: flex; justify-content: space-between;">
                                <span>Monnaie:&nbsp;</span>
                                <span> %s FCFA</span>
                            </div>
                        </div>

                        <!-- Ligne de séparation -->
                        <div style="border-top: 1px dashed #ccc; margin: 15px 0;"></div>

                        <!-- Footer -->
                        <div style="text-align: center; font-size: 8px; color: #999; margin-top: 15px;">
                            <p style="margin: 5px 0;">Généré par Tchakeda</p>
                            <p style="margin: 5px 0; font-size: 6px;">Adresse : Faladiè Sema, Rue du Gouverneur, près de l'hôtel Fiesta /Bamako-Mali</p>
                        </div>
                    </div>
                </body>
            </html>
            """, 
            request.getNumeroFacture(),
            formatDateForDisplay(request.getDateVente()),
            request.getNomVendeur(),
            request.getNomBoutique(),
            colonneRemiseHeader,
            lignesHtml.toString(),
            remisesSection.toString(),
            montantTotalFormate,
            request.getModePaiement().toUpperCase(),
            montantPayeFormate,
            changeDueFormate
        );
    }
    private String formatDateForDisplay(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    /**
     * Encode l'ID de la facture comme le fait le frontend Angular
     * Format: t_ + base64(encodeURIComponent(id))
     * Pour un ID numérique simple, cela équivaut à: t_ + base64(id.toString())
     */
    private String encodeFactureId(Long factureId) {
        String idString = String.valueOf(factureId);
        String base64Encoded = Base64.getEncoder().encodeToString(idString.getBytes(StandardCharsets.UTF_8));
        return "t_" + base64Encoded;
    }

    /**
     * Détecte l'URL frontend appropriée selon l'origine de la requête

     */
    private String detectFrontendUrl(HttpServletRequest request) {
        if (request == null) {
            return frontendUrl;
        }

        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isBlank()) {
                try {
                    java.net.URI uri = new java.net.URI(referer);
                    String scheme = uri.getScheme();
                    String host = uri.getHost();
                    int port = uri.getPort();
                    if (scheme != null && host != null) {
                        origin = scheme + "://" + host + (port != -1 ? ":" + port : "");
                    }
                } catch (Exception e) {
                    logger.debug("Impossible de parser le Referer: {}", referer);
                }
            }
        }

        if (origin != null && !origin.isBlank()) {
            if (origin.startsWith("https://fere.tchakeda.com") || origin.startsWith("https://www.fere.tchakeda.com")) {
                if (origin.contains("www.")) {
                    return "https://www.fere.tchakeda.com";
                } else {
                    return "https://fere.tchakeda.com";
                }
            } else if (origin.startsWith("https://xpertcash.tchakeda.com")) {
                return "https://xpertcash.tchakeda.com";
            } else if (origin.startsWith("http://192.168.1.5:4200") || origin.startsWith("http://localhost:4200")) {
                return origin;
            }
        }
        return frontendUrl;
    }

}
