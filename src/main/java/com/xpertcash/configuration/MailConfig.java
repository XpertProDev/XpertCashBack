package com.xpertcash.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.facture.host}")
    private String factureHost;

    @Value("${spring.mail.facture.port}")
    private int facturePort;

    @Value("${spring.mail.facture.username}")
    private String factureUsername;

    @Value("${spring.mail.facture.password}")
    private String facturePassword;

    @Bean
    public JavaMailSender javaMailSender() {
        return createMailSender(host, port, username, password);
    }

    @Bean("factureMailSender")
    public JavaMailSender factureMailSender() {
        return createMailSender(factureHost, facturePort, factureUsername, facturePassword);
    }

    private JavaMailSender createMailSender(String host, int port, String username, String password) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        
        // Configuration SSL pour le port 465
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.trust", host);
        } else {
            // Configuration STARTTLS pour les autres ports (587, etc.)
        props.put("mail.smtp.starttls.enable", "true");
        }

        return mailSender;
    }
}
