package com.xpertcash.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.FactureProForma;
import org.springframework.web.multipart.MultipartFile;


@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;
 
    @Value("${spring.mail.username}")
    private String from;

    public void sendActivationLinkEmail(String to, String code, String personalCode) throws MessagingException {
        String baseUrl = "http://localhost:8080"; // Adaptez cette URL à votre environnement
        String activationUrl = baseUrl + "/api/auth/activate?email=" + to + "&code=" + code;

        String subject = "Activation de votre compte";
        String htmlContent = generateActivationEmail(personalCode, activationUrl);

        sendEmail(to, subject, htmlContent);
    }


    public void sendEmployeEmail(String to, String fullName, String companyName, String role, String email, String password, String personalCode) throws MessagingException {
        String subject = "Création de votre compte XpertCash";
        String htmlContent = generateInfoEmail(fullName, companyName, role, email, password, personalCode);
        sendEmail(to, subject, htmlContent);
    }
    
    
     // Méthode d'envoi d'email pour relancer une facture
     public void sendRelanceeEmail(String to, String fullName, String factureNumero, String clientName, Date relanceDate, boolean estEntreprise) throws MessagingException {
        System.out.println("📧 Envoi d'un email à : " + to);
        String subject = "Relance de la facture " + factureNumero;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formattedDate = sdf.format(relanceDate);
        String message = generateFactureRelanceMessage(factureNumero, clientName, formattedDate, estEntreprise);
        sendEmail(to, subject, message);
    }
    


    public void sendUnlockLinkEmail(String to, String code) throws MessagingException {
        String baseUrl = "http://localhost:8080";
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
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setFrom(from);
    helper.setTo(toEmail);
    helper.setSubject(subject);
    helper.setText(htmlContent, true);

    try {
        InputStream logoStream = getClass().getClassLoader().getResourceAsStream("assets/logoxpertpro.png");
        if (logoStream == null) {
            throw new MessagingException("Logo image not found in resources.");
        }

        ByteArrayDataSource logoDataSource = new ByteArrayDataSource(logoStream, "image/png");
        helper.addInline("logo", logoDataSource);
    } catch (IOException e) {
        throw new MessagingException("Error loading logo image", e);
    }

    mailSender.send(message);
}


    public void sendPasswordResetEmail(String to, String otp) throws MessagingException {
        String subject = "Réinitialisation de votre mot de passe";
        String htmlContent = generatePasswordResetEmail(otp);
    
        sendEmail(to, subject, htmlContent);
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
                <div style="background: #f8f9fa; padding: 10px; border-radius: 5px; display: inline-block; text-align: left;">
                    <p><strong>Email :</strong> %s</p>
                    <p><strong>Mot de passe :</strong> %s</p>
                    <p><strong>Code PIN :</strong> %s</p>
                </div>
                <p style="margin-top: 8px;">Nous vous recommandons de changer votre mot de passe dès votre première connexion.</p>
                <a href="http://localhost:8080/login" style="display: inline-block; padding: 12px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 10px;">
                    Se connecter
                </a>
                <p style="font-size: 10px; color: #777;">L'équipe Tchakeda</p>
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
                </div>
            </body>
            </html>
        """.formatted(factureNumero, destinataireLabel, clientName, relanceDate);
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

    helper.setFrom(from);
    helper.setTo(toEmail.split(","));
    if (ccEmail != null && !ccEmail.isBlank()) {
        helper.setCc(ccEmail.split(","));
    }
    helper.setSubject(subject);
    helper.setText(htmlContent, true);

    InputStream logoStream = getClass().getClassLoader().getResourceAsStream("assets/logoxpertpro.png");
    if (logoStream != null) {
        ByteArrayDataSource logoDataSource = new ByteArrayDataSource(logoStream, "image/png");
        helper.addInline("logo", logoDataSource);
    } else {
        throw new MessagingException("Logo introuvable dans les resources.");
    }

    for (MultipartFile file : attachments) {
        if (!file.isEmpty()) {
            helper.addAttachment(
                Objects.requireNonNull(file.getOriginalFilename()),
                new ByteArrayResource(file.getBytes()),
                file.getContentType()
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
                    %s, %s
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

}
