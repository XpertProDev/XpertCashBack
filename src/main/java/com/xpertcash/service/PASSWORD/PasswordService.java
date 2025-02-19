package com.xpertcash.service.PASSWORD;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.User;
import com.xpertcash.entity.PASSWORD.PasswordResetToken;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.PASSWORD.PasswordResetTokenRepository;
import com.xpertcash.service.MailService;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;

@Service
public class PasswordService {

     @Autowired
     private UsersRepository usersRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

   @Autowired
    private MailService mailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Générer un code OTP (6 chiffres)
    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(1000000));
    }


        // Étape 1 : Générer et envoyer le code de réinitialisation
        @Transactional
        public void generateResetToken(String email) {
    User user = usersRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Aucun compte associé à cet email."));

    // Générer un nouveau code OTP
    String otp = generateOTP();
    LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(10);

    // Vérifier si un token existe déjà pour cet email
    PasswordResetToken resetToken = passwordResetTokenRepository.findByEmail(email)
            .orElse(new PasswordResetToken());

    // Mettre à jour ou créer un nouveau token
    resetToken.setEmail(email);
    resetToken.setToken(otp);
    resetToken.setExpirationDate(expirationDate);

    // Sauvegarde du token mis à jour
    passwordResetTokenRepository.save(resetToken);

    // Envoyer l'email
    sendResetEmail(email, otp);
}


        // Envoi de l'email avec le code OTP
        private void sendResetEmail(String email, String otp) {
                try {
                    mailService.sendPasswordResetEmail(email, otp);
                } catch (MessagingException e) {
                    throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation", e);
                }
        }       




    // Étape : Réinitialiser le mot de passe
    @Transactional
    public void resetPassword(String email, String token, String newPassword) {
        // Récupérer le token pour vérifier si il est valide
        PasswordResetToken resetToken = passwordResetTokenRepository.findByEmailAndToken(email, token)
                .orElseThrow(() -> new RuntimeException("Code invalide ou expiré."));
    
        // Vérifier si le token a expiré
        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le code de vérification a expiré.");
        }
    
        // Récupérer l'utilisateur
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));
    
        // Changer le mot de passe de l'utilisateur
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);
    
        // Supprimer le token après utilisation pour empêcher son usage multiple
        passwordResetTokenRepository.deleteByEmail(email);
    }
    

}
