package com.xpertcash.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    // Injection de dependance
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    // Méthode de test pour vérifier l'envoi d'email
    /*@PostConstruct
    public void testEmail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo("sydiakaridia38@gmail.com");
            message.setSubject("Test d'envoi d'email");
            message.setText("Ceci est un email de test.");
            mailSender.send(message);
            System.out.println("✅ Email de test envoyé avec succès.");
        } catch (MailException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email de test : " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    // Vos méthodes existantes pour l'envoi d'email...
    public void sendActivationLinkEmail(String to, String code) {
        String baseUrl = "http://localhost:8080"; // Adaptez cette URL à votre environnement
        String activationUrl = baseUrl + "/api/auth/activate?email=" + to + "&code=" + code;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Activation de votre compte");
        message.setText("Bonjour,\n\nLors de votre inscription, vous bénéficiez de 24h d'utilisation gratuite du système.\n" +
                "Pour continuer à utiliser votre compte au-delà de cette période, veuillez l'activer en cliquant sur le lien suivant :\n" +
                activationUrl + "\n\nCe code PIN (4 chiffres) vous a été généré automatiquement lors de votre inscription.");
        mailSender.send(message);
    }

    public void sendUnlockLinkEmail(String to, String code) {
        String baseUrl = "http://localhost:8080";
        String unlockUrl = baseUrl + "/api/auth/unlock?email=" + to + "&code=" + code;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Déverrouillage de votre compte");
        message.setText("Votre compte a été verrouillé en raison d'une inactivité de 30 minutes.\n" +
                "Pour le déverrouiller, cliquez sur le lien suivant :\n" + unlockUrl);
        mailSender.send(message);
    }

    // Envoie un email contenant le code de vérification à l'adresse spécifiée.
    public void sendEmailVerificationCode(String toEmail, String verificationCode) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("Vérification de changement d'email");
        mailMessage.setText("Votre code de vérification pour changer votre email est : " + verificationCode);
        mailSender.send(mailMessage);
    }

    public void sendEmail(String toEmail, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }
}
