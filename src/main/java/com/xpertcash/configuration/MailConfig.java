package com.xpertcash.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    // Configuration pour l'email contact (inscriptions et autres)
    @Value("${spring.mail.contact.host}")
    private String contactHost;

    @Value("${spring.mail.contact.port}")
    private int contactPort;

    @Value("${spring.mail.contact.username}")
    private String contactUsername;

    @Value("${spring.mail.contact.password}")
    private String contactPassword;

    // Configuration pour l'email facture
    @Value("${spring.mail.facture.host}")
    private String factureHost;

    @Value("${spring.mail.facture.port}")
    private int facturePort;

    @Value("${spring.mail.facture.username}")
    private String factureUsername;

    @Value("${spring.mail.facture.password}")
    private String facturePassword;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return createMailSender(contactHost, contactPort, contactUsername, contactPassword);
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
            props.put("mail.transport.protocol", "smtps");
        } else {
        props.put("mail.smtp.starttls.enable", "true");
        }
        
        // Timeouts 
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return mailSender;
    }
}
