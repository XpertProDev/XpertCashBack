package com.xpertcash.service.PASSWORD;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
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

    String otp = generateOTP();
    LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(10);

    // Vérifier si un token existe déjà pour cet utilisateur
    PasswordResetToken resetToken = passwordResetTokenRepository.findByUser(user)
            .orElse(new PasswordResetToken());

    // Mettre à jour ou créer un nouveau token
    resetToken.setUser(user);
    resetToken.setToken(otp);
    resetToken.setExpirationDate(expirationDate);

    passwordResetTokenRepository.save(resetToken);

    // Envoyer l'email à partir de l'utilisateur
    sendResetEmail(user, otp);
}

    // Envoi de l'email avec le code OTP
private void sendResetEmail(User user, String otp) {
    try {
        mailService.sendPasswordResetEmail(user.getEmail(), otp);
    } catch (MessagingException e) {
        throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation", e);
    }
}


    // Étape 2 : Réinitialiser le mot de passe
    @Transactional
    public void resetPassword(String token, String newPassword) {
    PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Code invalide ou expiré."));

    if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("Le code de vérification a expiré.");
    }

    User user = resetToken.getUser();
    if (user == null) {
        throw new RuntimeException("Utilisateur non trouvé.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    usersRepository.save(user);

    passwordResetTokenRepository.deleteByUser(user);
}

    public PasswordResetToken validateOtp(String email, String code) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte associé à cet email."));

        PasswordResetToken resetToken = passwordResetTokenRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Aucun code de réinitialisation trouvé."));

        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le code de vérification a expiré.");
        }

        if (!resetToken.getToken().equals(code)) {
            throw new RuntimeException("Code invalide.");
        }

        return resetToken;
    }

}
