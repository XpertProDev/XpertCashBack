package com.xpertcash.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


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

    public void sendEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); 

        FileSystemResource logoResource = new FileSystemResource("src/main/resources/assets/logoxpertpro.png");
        helper.addInline("logo", logoResource);

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
                            <p style="font-size: 10px; color: #777;">L'équipe XpertCash</p>
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
                <p style="font-size: 10px; color: #777;">L'équipe XpertCash</p>
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
                       <p style="font-size: 10px; color: #777;">L'équipe XpertCash</p>
                    </div>
                </body>
            </html>
        """.formatted(otp);
    }



}
